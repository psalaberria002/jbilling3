/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package jbilling

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.common.Constants
import com.sapienter.jbilling.common.SessionInternalError

import com.sapienter.jbilling.server.item.CurrencyBL

import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserStatusDAS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugins.springsecurity.Secured

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.security.authentication.encoding.PasswordEncoder

import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import com.sapienter.jbilling.client.util.DownloadHelper

import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.server.user.contact.db.ContactDTO

import com.sapienter.jbilling.client.util.SortableCriteria

import org.hibernate.FetchMode
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Subqueries
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Criterion
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.contact.db.ContactDAS

import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import org.apache.commons.lang.StringUtils
import com.sapienter.jbilling.server.user.contact.db.ContactFieldTypeDAS

@Secured(["MENU_90"])
class CustomerController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils
    PasswordEncoder passwordEncoder

    def filterService
    def recentItemService
    def breadcrumbService
    def springSecurityService

    @Secured(["hasAnyRole('MENU_90', 'CUSTOMER_15')"])
    def index = {
        redirect action: list, params: params
    }

    def getList(filters, statuses, GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        return UserDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            createAlias("contact", "contact")
            createAlias("customer", "customer")
            and {
                filters.each { filter ->
                    //log.debug "Filter value: '${filter.field}'"
                    if (filter.value != null) {
                        // handle user status separately from the other constraints
                        // we need to find the UserStatusDTO to compare to
                        if (filter.constraintType == FilterConstraint.STATUS) {
                            eq("userStatus", statuses.find{ it.id == filter.integerValue })
                        } else if (filter.field == 'contact.fields') {
                            String typeId = params['contactFieldTypes']
                            String ccfValue= filter.stringValue
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            if (typeId && ccfValue) {
                                createAlias("contact.fields", "field")
                                createAlias("field.type", "type")
                                setFetchMode("type", FetchMode.JOIN)
                                eq("type.id", typeId.toInteger())
                                addToCriteria(Restrictions.ilike("field.content", ccfValue, MatchMode.ANYWHERE))
                            }
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }
                //check that the user is a customer
                isNotNull('customer')
                eq('company', new CompanyDTO(session['company_id']))
            }
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    /**
     * Get a list of users and render the list page. If the "applyFilters" parameter is given, the
     * partial "_users.gsp" template will be rendered instead of the complete user list.
     */
    @Secured(["hasAnyRole('MENU_90', 'CUSTOMER_15')"])
    def list = {
        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findAll()
        def users = []

        // if logged in as a customer, you can only view yourself
        if (SpringSecurityUtils.ifNotGranted("MENU_90")) {
            users << UserDTO.get(springSecurityService.principal.id)
        } else {
            users = getList(filters, statuses, params)
        }

        def selected = params.id ? UserDTO.get(params.int("id")) : null
        def contact = null
        def revenue = null
        def latestOrder = null
        def latestPayment = null
        def latestInvoice = null

        if (selected) {
            contact = ContactDTO.findByUserId(selected.id)
            revenue = webServicesSession.getTotalRevenueByUser(selected.userId)
            latestOrder = webServicesSession.getLatestOrder(selected.userId)
            latestPayment = webServicesSession.getLatestPayment(selected.userId)
            latestInvoice = webServicesSession.getLatestInvoice(selected.userId)
        }

        def crumbDescription = selected ? UserHelper.getDisplayName(selected, contact) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, crumbDescription)

        if (params.applyFilter || params.partial) {
            render template: 'customers', model: [  selected: selected, contact: contact, revenue: revenue, latestOrder: latestOrder, latestPayment: latestPayment, latestInvoice: latestInvoice,
                                                    users: users, statuses: statuses, filters: filters ]
        } else {
            render view: 'list', model: [ selected: selected, contact: contact, revenue: revenue, latestOrder: latestOrder, latestPayment: latestPayment, latestInvoice: latestInvoice,
                                          users: users, statuses: statuses, filters: filters ]
        }
    }

    /**
     * Applies the set filters to the user list, and exports it as a CSV for download.
     */
    @Secured(["CUSTOMER_16"])
    def csv = {
        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findAll()

        params.max = CsvExporter.MAX_RESULTS
        def users = getList(filters, statuses, params)

        if (users.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list'

        } else {
            DownloadHelper.setResponseHeader(response, "users.csv")
            Exporter<UserDTO> exporter = CsvExporter.createExporter(UserDTO.class);
            render text: exporter.export(users), contentType: "text/csv"
        }
    }

    /**
     * Show details of the selected user. By default, this action renders the "_show.gsp" template.
     * When rendering for an AJAX request the template defined by the "template" parameter will be rendered.
     */
    @Secured(["CUSTOMER_15"])
    def show = {
        def user = UserDTO.get(params.int('id'))
        if (!user) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
        def contact = new ContactDAS().findPrimaryContact(user.userId)

        def revenue = webServicesSession.getTotalRevenueByUser(user.userId)
        def latestOrder = webServicesSession.getLatestOrder(user.userId)
        def latestPayment = webServicesSession.getLatestPayment(user.userId)
        def latestInvoice = webServicesSession.getLatestInvoice(user.userId)
		def x=webServicesSession.getOneTimersAndMANDSWithDescription(user.userId)
		def maintenanceAndSupport=x.getAt(0)
		def licensesAndSetup=x.getAt(1)
		println x.size()
		println licensesAndSetup
		

        recentItemService.addRecentItem(user.userId, RecentItemType.CUSTOMER)
        breadcrumbService.addBreadcrumb(controllerName, 'list', params.template ?: null, user.userId, UserHelper.getDisplayName(user, contact))

        render template: params.template ?: 'show', model: [ selected: user, contact: contact, revenue: revenue, latestOrder: latestOrder, latestPayment: latestPayment, latestInvoice: latestInvoice ,maintenanceAndSupport:maintenanceAndSupport, licensesAndSetup:licensesAndSetup]
    }

    /**
     * Fetches a list of sub-accounts for the given user id and renders the user list "_table.gsp" template.
     */
    def subaccounts = {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def children = UserDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            customer {
                parent {
                    eq('baseUser.id', params.int('id'))
                }
            }
        }

        def parent = UserDTO.get(params.int('id'))
        render template: 'customers', model: [ users: children, parent: parent ]
    }

    /**
     * Updates the notes for the given user id.
     */
    @Secured(["CUSTOMER_11"])
    def saveNotes = {
        if (params.id) {
            try {
                webServicesSession.saveCustomerNotes(params.int('id'), params.notes)

                log.debug("Updating notes for user ${params.id}.")

                flash.message = 'customer.notes'
                flash.args = [params.id as String]
            } catch (SessionInternalError e) {
                log.error("Could not save the customer's notes", e)

                viewUtils.resolveException(flash, session.locale, e)
            }
        }

        // render user list with selected id
        redirect action: "list", id: params.id
    }

    /**
     * Delete the given user id.
     */
    @Secured(["CUSTOMER_12"])
    def delete = {
        if (params.id) {
            webServicesSession.deleteUser(params.int('id'))

            flash.message = 'customer.deleted'
            flash.args = [ params.id ]
            log.debug("Deleted user ${params.id}.")

        }

        // render the partial user list
        params.partial = true
        list()
    }

    /**
     * Get the user to be edited and show the "edit.gsp" view. If no ID is given this view
     * will allow creation of a new user.
     */
    @Secured(["hasAnyRole('CUSTOMER_10', 'CUSTOMER_11')"])
    def edit = {
        def user
        def contacts
        def parent

        try {
            user = params.id ? webServicesSession.getUserWS(params.int('id')) : null
            contacts = user ? webServicesSession.getUserContactsWS(user.userId) : null
            parent = params.parentId ? webServicesSession.getUserWS(params.int('parentId')) : null

        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)

            flash.error = 'customer.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'customer', action: 'list'
            return
        }

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? UserHelper.getDisplayName(user, user.contact) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)

        def periodUnits = PeriodUnitDTO.list()

        [ user: user, contacts: contacts, parent: parent, company: company, currencies: currencies, periodUnits:periodUnits ]
    }

    /**
     * Validate and save a user.
     */
    @Secured(["hasAnyRole('CUSTOMER_10', 'CUSTOMER_11')"])
    def save = {
        def user = new UserWS()
        def contactFieldType = new ContactFieldTypeDAS();
        params.contactField.each {field ->
        if (contactFieldType.find(new Integer(field.key)).dataType == 'Decimal'){
            try {
            if(field.value)
            Double.parseDouble(field.value)
            }
            catch(NumberFormatException e){
            flash.error = 'custom.contact.field.decimal'
            flash.args = [contactFieldType.find(new Integer(field.key)).description,'Decimal']
             redirect action: 'edit' , params: [id : params.user.userId]
            }
        }
        else if (contactFieldType.find(new Integer(field.key)).dataType == 'Integer'){
            try {
            if(field.value)
            Long.parseLong(field.value)
            }
            catch(NumberFormatException e){
            flash.error = 'custom.contact.field.integer'
            flash.args = [contactFieldType.find(new Integer(field.key)).description,'Integer']
            redirect action: 'edit' , params: [id : params.user.userId]
            }
        }
        else if (contactFieldType.find(new Integer(field.key)).dataType == 'Boolean'){
            if(field.value && !(field.value.equalsIgnoreCase('true') || field.value.equalsIgnoreCase('false')))
            {
            flash.error = 'custom.contact.field.boolean'
            flash.args = [contactFieldType.find(new Integer(field.key)).description,'Boolean']
            redirect action: 'edit' , params: [id : params.user.userId]
            }
        }
        }

        UserHelper.bindUser(user, params)

        def contacts = []
        UserHelper.bindContacts(user, contacts, company, params)

        def oldUser = (user.userId && user.userId != 0) ? webServicesSession.getUserWS(user.userId) : null
        UserHelper.bindPassword(user, oldUser, params, flash)

        if (flash.error) {
            render view: 'edit', model: [user: user, contacts: contacts, company: company]
            return
        }

        try {
            // save or update
            if (!oldUser) {
                if (SpringSecurityUtils.ifAllGranted("CUSTOMER_10")) {
                    if (user.userName.trim()) {
                        user.userId = webServicesSession.createUser(user)

                        flash.message = 'customer.created'
                        flash.args = [user.userId as String]
                    } else {
                        user.userName = StringUtils.EMPTY
                        flash.error = message(code: 'customer.error.name.blank')

                        render view: "edit", model: [user: user, contacts: contacts, parent: null, company: company, currencies: currencies, periodUnits: PeriodUnitDTO.list()]
                        return
                    }
                } else {
                    render view: '/login/denied'
                    return
                }

            } else {
                if (SpringSecurityUtils.ifAllGranted("CUSTOMER_11")) {

                    webServicesSession.updateUser(user)

                    // ACH updates are not handled through updateUser. Make a separate API call
                    // to update the customers ACH data if it's present
                    if (user.ach) {
                        webServicesSession.updateAch(user.userId, user.ach)
                    }

                    // payment data deletions
                    if (params.deleteAch) {
                        log.debug("deleting ACH for user ${user.userId}")
                        webServicesSession.deleteAch(user.userId)
                    }

                    if (params.deleteCreditCard) {
                        log.debug("deleting Credit Card for user ${user.userId}")
                        webServicesSession.deleteCreditCard(user.userId)
                    }

                    flash.message = 'customer.updated'
                    flash.args = [user.userId as String]

                } else {
                    render view: '/login/denied'
                    return
                }
            }

            // save contacts
            if (user.userId) {
                contacts.each {
                    webServicesSession.updateUserContact(user.userId, it.type, it);
                }
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [user: user, contacts: contacts, company: company, currencies: currencies]
            return
        }

        chain action: 'list', params: [id: user.userId]
    }

    def getCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll { it.inUse }
    }

    def getCompany() {
        CompanyDTO.get(session['company_id'])
    }
}
