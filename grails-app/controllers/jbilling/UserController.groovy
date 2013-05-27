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
import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO
import com.sapienter.jbilling.server.user.permisson.db.PermissionTypeDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugins.springsecurity.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.FetchMode as FM

@Secured(["MENU_99"])
class UserController {

    static pagination = [ max: 10, offset: 0 ]

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def breadcrumbService


    def index = {
        redirect action: list, params: params
    }

    def getList(GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        return UserDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {


            and {
                or {
                    isEmpty('roles')
                    roles {
                        ne('roleTypeId', Constants.TYPE_CUSTOMER)
                    }
                }

                eq('company', new CompanyDTO(session['company_id']))
                eq('deleted', 0)
            }
            order('id', 'desc')
        }
    }

    def list = {
        def users = getList(params)
        def selected = params.id ? UserDTO.get(params.int("id")) : null
        def contact = selected ? ContactDTO.findByUserId(selected.id) : null

        def crumbDescription = selected ? UserHelper.getDisplayName(selected, contact) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, crumbDescription)

        if (params.applyFilter || params.partial) {
            render template: 'users', model: [ users: users, selected: selected, contact: contact ]
        } else {
            [ users: users, selected: selected, contact: contact ]
        }
    }

    def show = {
        def user = UserDTO.get(params.int('id'))
        def contact = user ? ContactDTO.findByUserId(user.id) : null

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, user.userId, UserHelper.getDisplayName(user, contact))

        render template: 'show', model: [ selected: user, contact: contact ]
    }

    def edit = {
        def user
        def contacts

        try {
            user = params.id ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
            contacts = params.id ? webServicesSession.getUserContactsWS(user.userId) : null

        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)

            flash.error = 'user.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'user', action: 'list'
            return
        }
		
		def company = CompanyDTO.createCriteria().get{
			 eq("id", session['company_id'])
			 fetchMode('contactFieldTypes', FM.JOIN)
		}
		
		def roles = RoleDTO.createCriteria().list() {
			eq('company', new CompanyDTO(session['company_id']))
			order('id', 'asc')
		}

        [ user: user, contacts: contacts, company: company, roles: roles ]
    }


    /**
     * Validate and save a user.
     */
    def save = {
        def user = new UserWS()
        user.mainRoleId= Constants.TYPE_ROOT
        UserHelper.bindUser(user, params)

        def contacts = []

		def company = CompanyDTO.createCriteria().get{
			 eq("id", session['company_id'])
			 fetchMode('contactFieldTypes', FM.JOIN)
		}
		
        UserHelper.bindContacts(user, contacts, company, params)

        def oldUser = (user.userId && user.userId != 0) ? webServicesSession.getUserWS(user.userId) : null
        UserHelper.bindPassword(user, oldUser, params, flash)

        if (flash.error) {
            render view: 'edit', model: [ user: user, contacts: contacts, company: company ]
            return
        }
		
		
        try {
            // save or update
            if (!oldUser) {
                log.debug("creating user ${user}")

                user.userId = webServicesSession.createUser(user)
				
				webServicesSession.updateOrInsertUserPass(user.userId,params.newPassword)//Insert not encrypted password in user_pass

                flash.message = 'user.created'
                flash.args = [ user.userId as String ]

            } else {
                log.debug("saving changes to user ${user.userId}")

                webServicesSession.updateUser(user)
				
				if(!params.oldPassword.equals(params.newPassword)){
					webServicesSession.updateOrInsertUserPass(user.userId,params.newPassword)//Update not encrypted password in user_pass
				}
				

                flash.message = 'user.updated'
                flash.args = [ user.userId as String ]
            }

            // save secondary contacts
            if (user.userId) {
                contacts.each{
                    webServicesSession.updateUserContact(user.userId, it.type, it);
                }
            }

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render view: 'edit', model: [ user: user, contacts: contacts, company: company ]
            return
        }

        chain action: 'list', params: [ id: user.userId ]
    }

    def delete = {
        if (params.id) {
            webServicesSession.deleteUser(params.int('id'))
            log.debug("Deleted user ${params.id}.")
        }

        flash.message = 'user.deleted'
        flash.args = [ params.id as String ]

        // render the partial user list
        params.applyFilter = true
        list()
    }
    
}
