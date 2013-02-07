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

import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO
import com.sapienter.jbilling.server.payment.IPaymentSessionBean
import com.sapienter.jbilling.server.util.Context
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.UserBL
import grails.plugins.springsecurity.Secured

@Secured(["CUSTOMER_14"])
class BlacklistController {

    def index = {
        redirect action: list, params: params
    }

    def getFilteredList(GrailsParameterMap params) {
        def blacklist = BlacklistDTO.createCriteria().list() {

            if (params.filterBy && params.filterBy != message(code: 'blacklist.filter.by.default')) {
                or {
                    eq('user.id', params.int('filterBy'))
                    user {
                        ilike('userName', "%${params.filterBy}%")
                    }
                    creditCard {
                        ilike('ccNumberPlain', "%${params.filterBy}%")
                    }
                }
            }

            eq('company.id', session['company_id'])
            order('id', 'asc')
        }

    }

    def list = {
        def blacklist = getFilteredList(params)
        def selected = params.id ? BlacklistDTO.get(params.int('id')) : null

        [ blacklist: blacklist, selected: selected ]
    }

    def filter = {
        def blacklist = getFilteredList(params)

        render template: 'entryList', model: [ blacklist: blacklist ]
    }

    def show = {
        def entry = BlacklistDTO.get(params.int('id'))

        render template: 'show', model: [ selected: entry ]
    }

    def save = {
        def replace = params.csvUpload == 'modify'
        def file = request.getFile('csv');
        if(!params.csv.getContentType().toString().contains('application/octet-stream')){
            flash.error = "csv.error.found"
            redirect action: 'list'
            }
        if (!file.empty) {
            def csvFile = File.createTempFile("blacklist", ".csv")
            file.transferTo(csvFile)

            IPaymentSessionBean paymentSession = Context.getBean(Context.Name.PAYMENT_SESSION)
            def added = paymentSession.processCsvBlacklist(csvFile.getAbsolutePath(), replace, (Integer) session['company_id'])

            flash.message = replace ? 'blacklist.updated' : 'blacklist.added'
            flash.args = [ added ]
        }

        redirect view: 'list'
    }

    def user = {
        if (params.id) {
            def bl = new UserBL(params.int('id'))
            bl.setUserBlacklisted((Integer) session['user_id'], true)

            flash.message = 'user.blacklisted'
            flash.args = [ params.id as String ]
        }

        redirect controller: 'customerInspector', action: 'inspect', id: params.id
    }

}
