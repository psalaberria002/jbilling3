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

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.plugins.springsecurity.Secured
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * BillingController
 *
 * @author Vikas Bodani
 * @since
 */
@Secured(["MENU_91"])
class InvoiceController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    def webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService

    def index = {
        redirect action: 'list', params: params
    }

    def list = {
        def filters = filterService.getFilters(FilterType.INVOICE, params)
        def invoices = getInvoices(filters, params)

        def selected = params.id ? InvoiceDTO.get(params.int('id')) : null

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, params.int('id'))

        if (params.applyFilter || params.partial) {
            render template: 'invoices', model: [invoices: invoices, filters: filters, selected: selected, currencies: currencies]
        } else {
            [invoices: invoices, filters: filters, selected: selected, currencies: currencies]
        }
    }

    def getInvoices(filters, GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        // hide review invoices by default
        def reviewFilter = filters.find { it.field == 'isReview' }
        if (reviewFilter && reviewFilter.value == null) reviewFilter.integerValue = Integer.valueOf(0)

        // get list
        return InvoiceDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ) {
            and {
                filters.each { filter ->
                    if (filter.value != null) {
                        //handle invoiceStatus
                        if (filter.field == 'invoiceStatus') {
                            def statuses = new InvoiceStatusDAS().findAll()
                            eq("invoiceStatus", statuses.find { it.primaryKey?.equals(filter.integerValue) })
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }

                createAlias('baseUser', 'baseUser')
                eq('baseUser.company', new CompanyDTO(session['company_id']))
                eq('deleted', 0)

                // limit list to only this customer's invoices
                if (SpringSecurityUtils.ifNotGranted("INVOICE_74")) {
                    eq('baseUser.id', session['user_id'])
                }
            }

            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    /**
     * Applies the set filters to the order list, and exports it as a CSV for download.
     */
    @Secured(["INVOICE_73"])
    def csv = {
        def filters = filterService.getFilters(FilterType.INVOICE, params)

        params.max = CsvExporter.MAX_RESULTS
        def invoices = getInvoices(filters, params)

        if (invoices.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "invoices.csv")
            Exporter<InvoiceDTO> exporter = CsvExporter.createExporter(InvoiceDTO.class);
            render text: exporter.export(invoices), contentType: "text/csv"
        }
    }

    /**
     * Convenience shortcut, this action shows all invoices for the given user id.
     */
    def user = {
        def filter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'baseUser.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, filter)

        redirect action: 'list'
    }

    @Secured(["INVOICE_72"])
    def show = {
        def invoice = InvoiceDTO.get(params.int('id'))
        if (!invoice) {
            log.debug("Redirecting to list")
            redirect(action: 'list')
            return
        }
        recentItemService.addRecentItem(invoice.id, RecentItemType.INVOICE)
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, invoice.id, invoice.number)

        render template: params.template ?: 'show', model: [selected: invoice, currencies: currencies]
    }

    def snapshot = {
        def invoiceId = params.int('id')
        if (invoiceId) {
            InvoiceWS invoice = webServicesSession.getInvoiceWS(invoiceId)
            render template: 'snapshot', model: [invoice: invoice, currencies: currencies]
        }
    }

    @Secured(["INVOICE_70"])
    def delete = {
        int invoiceId = params.int('id')
        int userId = params.int('_userId')

        if (invoiceId) {
            try {
                webServicesSession.deleteInvoice(invoiceId)
                flash.message = 'invoice.delete.success'
                flash.args = [invoiceId]

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                log.error("Exception deleting invoice.", e)
                flash.error = 'error.invoice.delete'
                flash.args = [params.id]
                redirect action: 'list', params: [id: userId]
                return
            }
        }

        redirect action: list, params: [id: userId]
    }

    @Secured(["INVOICE_71"])
    def email = {
        if (params.id) {
            try {
                def sent = webServicesSession.notifyInvoiceByEmail(params.int('id'))

                if (sent) {
                    flash.message = 'invoice.prompt.success.email.invoice'
                    flash.args = [params.id]
                } else {
                    flash.error = 'invoice.prompt.failure.email.invoice'
                    flash.args = [params.id]
                }

            } catch (Exception e) {
                log.error("Exception occurred sending invoice email", e)
                flash.error = 'invoice.prompt.failure.email.invoice'
                flash.args = [params.id]
            }
        }

        redirect action: 'list', params: [id: params.id]
    }

    def downloadPdf = {
        Integer invoiceId = params.int('id')

        try {
            byte[] pdfBytes = webServicesSession.getPaperInvoicePDF(invoiceId)
            def invoice = webServicesSession.getInvoiceWS(invoiceId)
            DownloadHelper.sendFile(response, "invoice-${invoice?.number}.pdf", "application/pdf", pdfBytes)

        } catch (Exception e) {
            log.error("Exception fetching PDF invoice data.", e)
            flash.error = 'invoice.prompt.failure.downloadPdf'
            redirect action: 'list', params: [id: invoiceId]
        }
    }

    @Secured(["PAYMENT_33"])
    def unlink = {
        try {
            webServicesSession.removePaymentLink(params.int('id'), params.int('paymentId'))
            flash.message = "payment.unlink.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception unlinking invoice.", e)
            flash.error = "error.invoice.unlink.payment"
        }

        redirect action: 'list', params: [id: params.id]
    }

    def byProcess = {
        if (!params.id) {
            flash.error = 'error.invoice.byprocess.missing.id'
            redirect action: 'list'
            return
        }

        // limit by billing process
        def processFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'billingProcess.id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, processFilter)

        // show review invoices if process generated a review
        def reviewFilter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'isReview', template: 'invoice/review', visible: true, integerValue: params.int('isReview'))
        filterService.setFilter(FilterType.INVOICE, reviewFilter, false)

        redirect action: list
    }

    def getCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll { it.inUse }
    }
	
	def saveNotes={
		if (!params.id) {
			flash.error = 'error.invoice.saveNotes.missing.id'
			redirect action: 'list'
			return
		}
		
		Integer invoiceId = params.int('id') as Integer
		InvoiceDAS das=new InvoiceDAS();
		println params
		println params.customerNotes
		InvoiceDTO dto=das.find(invoiceId)
		if(params.notes){
			dto.setCustomerNotes(params.notes);
			das.save(dto);
		}
		else{
			params.notes="";
			dto.setCustomerNotes(params.notes);
			das.save(dto);
		}
		session.message = 'Customer notes have been saved for invoice '+invoiceId
		redirect action: list
	}
}
