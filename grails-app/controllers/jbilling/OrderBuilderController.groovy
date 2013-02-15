/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package jbilling

import grails.plugins.springsecurity.Secured
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.user.db.UserDTO

import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.common.SessionInternalError
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import java.math.RoundingMode
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.jopendocument.dom.spreadsheet.Sheet
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.springframework.webflow.conversation.Conversation;
import org.apache.commons.lang.StringUtils
import com.sapienter.jbilling.server.item.CurrencyBL



/**
 * OrderController
 *
 * @author Brian Cowdery
 * @since 20-Jan-2011
 */
@Secured(["hasAnyRole('ORDER_20', 'ORDER_21')"])
class OrderBuilderController {

    def webServicesSession
    def viewUtils

    def breadcrumbService
    def productService

    def index = {
        redirect action: 'edit'
    }

    def editFlow = {
        /**
         * Initializes the order builder, putting necessary data into the flow and conversation
         * contexts so that it can be referenced later.
         */
        initialize {
            action {
                if (!params.id && !SpringSecurityUtils.ifAllGranted("ORDER_20")) {
                    // not allowed to create
                    redirect controller: 'login', action: 'denied'
                    return
                }

                if (params.id && !SpringSecurityUtils.ifAllGranted("ORDER_21")) {
                    // not allowed to edit
                    redirect controller: 'login', action: 'denied'
                    return
                }

                def order = params.id ? webServicesSession.getOrder(params.int('id')) : new OrderWS()

                if (!order) {
                    log.error("Could not fetch WS object")

                    session.error = 'order.not.found'
                    session.args = [ params.id ]

                    redirect controller: 'order', action: 'list'
                    return
                }

                def user = UserDTO.get(order?.userId ?: params.int('userId'))
                def contact = ContactDTO.findByUserId(order?.userId ?: user.id)

                def company = CompanyDTO.get(session['company_id'])
                def currencies = new CurrencyBL().getCurrencies(session['language_id'], session['company_id'])
                currencies = currencies.findAll{ it.inUse }

                // set sensible defaults for new orders
                if (!order.id || order.id == 0) {
                    order.userId        = user.id
                    order.currencyId    = (currencies.find{ it.id == user.currency.id} ?: company.currency).id
                    order.statusId      = Constants.ORDER_STATUS_ACTIVE
                    order.period        = Constants.ORDER_PERIOD_ONCE
                    order.billingTypeId = Constants.ORDER_BILLING_PRE_PAID
                    order.activeSince   = new Date()
                    order.isCurrent     = 0
                    order.orderLines    = []
                }

                // add breadcrumb for order editing
                if (params.id) {
                    breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))
                }

                // available order periods, statuses and order types
                def itemTypes = productService.getItemTypes()
                def orderStatuses = OrderStatusDTO.list().findAll { it.id != Constants.ORDER_STATUS_SUSPENDED_AGEING }
                def orderPeriods = company.orderPeriods.collect { new OrderPeriodDTO(it.id) } << new OrderPeriodDTO(Constants.ORDER_PERIOD_ONCE)
                orderPeriods.sort { it.id }
                def periodUnits = PeriodUnitDTO.list()

                def orderBillingTypes = [
                        new OrderBillingTypeDTO(Constants.ORDER_BILLING_PRE_PAID),
                        new OrderBillingTypeDTO(Constants.ORDER_BILLING_POST_PAID)
                ]

                // model scope for this flow
                flow.company = company
                flow.itemTypes = itemTypes
                flow.orderStatuses = orderStatuses
                flow.orderPeriods = orderPeriods
                flow.periodUnits = periodUnits
                flow.orderBillingTypes = orderBillingTypes
                flow.user = user
                flow.contact = contact

                // conversation scope
                conversation.order = order
                conversation.products = productService.getFilteredProducts(company, params)
                conversation.deletedLines = []
				
				
            }
            on("success").to("build")
        }

        /**
         * Renders the order details tab panel.
         */
        showDetails {
            action {
                params.template = 'details'
            }
            on("success").to("build")
        }

        /**
         * Renders the product list tab panel, filtering the product list by the given criteria.
         */
        showProducts {
            action {
                // filter using the first item type by default
                if (params['product.typeId'] == null && flow.itemTypes)
                    params['product.typeId'] = flow.itemTypes?.asList()?.first()?.id

                params.template = 'products'
                conversation.products = productService.getFilteredProducts(flow.company, params)
            }
            on("success").to("build")
        }

        /**
         * Adds a product to the order as a new order line and renders the review panel.
         */
        addOrderLine {
            action {

                // build line
                def line = new OrderLineWS()
                line.typeId = Constants.ORDER_LINE_TYPE_ITEM
                line.quantity = BigDecimal.ONE
                line.itemId = params.int('id')
                line.useItem = true

                // add line to order
                def order = conversation.order
                def lines = order.orderLines as List
                lines.add(line)
                order.orderLines = lines.toArray()

                // rate order
                if (order.orderLines) {
                    try {
                        order = webServicesSession.rateOrder(order)
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flow, session.locale, e)
                    }
                }
                conversation.order = order

                params.newLineIndex = lines.size() - 1
                params.template = 'review'
            }
            on("success").to("build")
        }
		

        /**
         * Updates an order line  and renders the review panel.
         */
        updateOrderLine {
            action {
                def order = conversation.order

                // get existing line
                def index = params.int('index')
                def line = order.orderLines[index]

                // update line
                bindData(line, params["line-${index}"])

                // must have a quantity
                if (!line.quantity) {
                    line.quantity = BigDecimal.ONE
                }

                // if product does not support decimals, drop scale of the given quantity
                def product = conversation.products?.find{ it.id == line.itemId }
                if (product?.hasDecimals == 0) {
                    line.quantity = line.getQuantityAsDecimal().setScale(0, RoundingMode.HALF_UP)
                }

                // existing line that's stored in the database will be deleted when quantity == 0

                if (line.quantityAsDecimal == BigDecimal.ZERO) {
                    log.debug("zero quantity, marking line to be deleted.")
                    line.deleted = 1
                    line.useItem = false

                    if (line.id != 0) {
                        // keep track of persisted lines so that we can make sure they're removed on save
                        conversation.deletedLines << line
                    }
                }

                // add line to order
                order.orderLines[index] = line

                // rate order
                if (order.orderLines) {
                    try {
                        order = webServicesSession.rateOrder(order)
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flow, session.locale, e)
                    }
                }

                // In case of a single order line having quantity set to zero, total of the order should be zero
                if(order.orderLines.size()==1 && order.orderLines[0].quantity=='0'){
                    order.total = BigDecimal.ZERO
                }

                // sort order lines
                order.orderLines = order.orderLines.sort { it.itemId }
                conversation.order = order

                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Removes a line from the order and renders the review panel.
         */
        removeOrderLine {
            action {
                def order = conversation.order

                def index = params.int('index')
                def lines = order.orderLines as List

                // remove or mark as deleted if already saved to the DB
                def line = lines.get(index)
                if (line.id != 0) {
                    log.debug("marking line ${line.id} to be deleted.")
                    line.deleted = 1
                    conversation.deletedLines << line

                } else {
                    log.debug("removing transient line from order.")
                lines.remove(index)
                }

                order.orderLines = lines.toArray()

                // rate order
                if (order.orderLines) {
                    try {
                        order = webServicesSession.rateOrder(order)
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flow, session.locale, e)
                    }
                } else {
                    order.setTotal(new BigDecimal(0))
                }

                conversation.order = order

                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Updates order attributes (period, billing type, active dates etc.) and
         * renders the order review panel.
         */
        updateOrder {
            action {
                def order = conversation.order
                bindData(order, params)

                order.isCurrent = params.isCurrent ? 1 : 0
                order.notify = params.notify ? 1 : 0
                order.notesInInvoice = params.notesInInvoice ? 1 : 0
				order.isMaster = params.isMaster ? 1 : 0
				order.addToMaster = params.addToMaster ? 1 : 0
				order.payPlan= params.plan 
				println params.period+" 1"
				println order.period+" 2"
				order.period = Integer.parseInt(params.period)
				println order.period+" 3"
				 
				

                // one time orders are ALWAYS post-paid
                if (order.period == Constants.ORDER_PERIOD_ONCE)
                    order.billingTypeId = Constants.ORDER_BILLING_POST_PAID
				
					

                // rate order
                if (order.orderLines) {
                    try {
                        order = webServicesSession.rateOrder(order)
                    } catch (SessionInternalError e) {
                        viewUtils.resolveException(flow, session.locale, e)
                    }
                }

                // sort order lines
                order.orderLines = order.orderLines.sort { it.itemId }
                conversation.order = order

                params.template = 'review'
            }
            on("success").to("build")
        }

        /**
         * Shows the order builder. This is the "waiting" state that branches out to the rest
         * of the flow. All AJAX actions and other states that build on the order should
         * return here when complete.
         *
         * If the parameter 'template' is set, then a partial view template will be rendered instead
         * of the complete 'build.gsp' page view (workaround for the lack of AJAX support in web-flow).
         */
        build {
            on("details").to("showDetails")
            on("products").to("showProducts")
            on("addLine").to("addOrderLine")
            on("updateLine").to("updateOrderLine")
            on("removeLine").to("removeOrderLine")
            on("update").to("updateOrder")

            on("save").to("saveOrder")
            // on("save").to("checkItem")  // check to see if an item exists, and show an information page before saving
            // on("save").to("beforeSave") // show an information page before saving

            on("cancel").to("finish")
        }

        /**
         * Example action that shows a static page before saving if the order contains
         * a Lemonade item.
         *
         * Uncomment the "save" to "checkItem" transition in the builder() state to use.
         */
        checkItem {
            action {
                def order = conversation.order
                if (order.orderLines.find{ it.itemId == 2602}) {
                    // order contains lemonade, show beforeSave.gsp
                    hasItem();
                } else {
                    // order does not contain lemonade, goto save
                    save();
                }
            }
            on("hasItem").to("beforeSave")
            on("save").to("saveOrder")
        }

        /**
         * Example action that shows a static page before the order is saved.
         *
         * Uncomment the "save" to "beforeSave" transition in the builder() state to use.
         */
        beforeSave {
            on("save").to("saveOrder")
            on("cancel").to("build")
        }

        /**
         * Saves the order and exits the builder flow.
         */
        saveOrder {
            action {
                try {
                    def order = conversation.order

                    if (!order.id || order.id == 0) {
                        if (SpringSecurityUtils.ifAllGranted("ORDER_20"))  {
							//Creating a new order. Not adding to the master order
							if(order.addToMaster != 1 ){
								println "create new order, not to master"
								log.debug("creating order ${order}")
								order.id = webServicesSession.createOrder(order)
								
								// set success message in session, contents of the flash scope doesn't survive
								// the redirect to the order list when the web-flow finishes
								session.message = 'order.created'
								session.args = [ order.id, order.userId ]
							}
							//Creating and adding a new order to the master order
							else {
								println "editOrders!!!"
								log.debug("creating edited order ${order}")
								def masterOrder = webServicesSession.getMasterOrder(order.userId)
								
								
									//Edit master order and new order
									editOrders(order,masterOrder,monthsLeft(order, masterOrder));
								
								
							
								order.id = webServicesSession.createUpdateOrder(order)

								// set success message in session, contents of the flash scope doesn't survive
								// the redirect to the order list when the web-flow finishes
								session.message = 'order.created'
								session.args = [ order.id, order.userId ]
							}
							
						}
                         else {
                            redirect controller: 'login', action: 'denied'
                        }
						

                    } else {
                        if (SpringSecurityUtils.ifAllGranted("ORDER_21")) {
							println "Editing"
                            // add deleted lines to our order so that updateOrder() can save them
                            def deletedLines = conversation.deletedLines
                            def lines = order.orderLines as List

                            log.debug "appending ${deletedLines.size()} line(s) for deletion."
                            lines.addAll(deletedLines)
                            order.orderLines = lines.toArray()

                            // save changes
                            log.debug("saving changes to order ${order.id}")
                            webServicesSession.updateOrder(order)

                            session.message = 'order.updated'
                            session.args = [ order.id, order.userId ]

                        } else {
                            redirect controller: 'login', action: 'denied'
                        }
                    }

                } catch (SessionInternalError e) {
                    viewUtils.resolveException(flow, session.locale, e)
                    error()
                }
            }
            on("error").to("build")
            on("success").to("finish")
        }

        finish {
            redirect controller: 'order', action: 'list', id: conversation.order?.id
        }
    }
	
	def int monthsLeft(order,masterOrder) {
		
		//Getting next billable day of the master order
		def masterOrderNextBillableDay = masterOrder.getNextBillableDay()
		Calendar cal = Calendar.getInstance();
		cal.setTime(masterOrderNextBillableDay);
		def monthNext = cal.get(Calendar.MONTH)+12;
		//Getting the starting day of the new order
		def orderActiveDay = order.getActiveSince()
		cal.setTime(orderActiveDay);
		def monthStart = cal.get(Calendar.MONTH);
		//Months for the new order
		return monthNext-monthStart
		
    }
	/**
	 * Having a new order, this method edits the master order with the new quantity, price and amount.
	 * Also edits the new order adding lines with the money back in case they have paid from before
	 * @param order
	 * @param masterOrder
	 * @param monthsLeft
	 * @return
	 */
	def  editOrders(order, masterOrder,monthsLeft){
		def back=0
		def masterOrderLines = masterOrder.getOrderLines()
		def newOrderLines = order.getOrderLines()
		def nolQuantity=0
		def molQuantity=0
		def totalQuantity=0
		newOrderLines.each { nol -> 
			totalQuantity=0
			boolean found=false
			nolQuantity=nol.getQuantityAsDecimal()
			masterOrderLines.each { mol ->
				if(found==false){
					if (nol.getItemId()==mol.getItemId()){
						found=true;
						molQuantity=mol.getQuantityAsDecimal()
						totalQuantity=molQuantity+nolQuantity
						def payPlan=masterOrder.getPayPlan()
						BigDecimal molOldAvgPrice = mol.getPriceAsDecimal()
						
						back=molOldAvgPrice*molQuantity*monthsLeft/12
						println back
						def file = new File("resources/pay_plans/${payPlan}.ods");
						def sheet = SpreadSheet.createFromFile(file).getSheet(0);
						BigDecimal value=sheet.getCellAt("B${totalQuantity.intValue()}").getValue()
						BigDecimal amount=sheet.getCellAt("C${totalQuantity.intValue()}").getValue()
						println value+" "+totalQuantity+" "+amount
						
						BigDecimal avgPrice=(BigDecimal)(amount/totalQuantity)
						println avgPrice+" avgPrice"
						//Edit master order line
						mol.setPrice(avgPrice)
						mol.setQuantityAsDecimal(totalQuantity)
						mol.setAmount(amount)
						mol.setDescription(mol.getDescription()+" "+monthsLeft+" months")
						
						webServicesSession.updateOrderLine(mol) //update Master Order Line
						println mol.getPrice()+" "+mol.getAmount()
						
						println "Order before"+order
						nol.quantity= totalQuantity
						nol.setAmount(amount*monthsLeft/12)
						nol.setPrice(amount*monthsLeft/12/totalQuantity)
						
						// build line
						def line = new OrderLineWS()
						line.typeId = Constants.ORDER_LINE_TYPE_ITEM
						line.quantity = (-1)*molQuantity
						line.setPrice(molOldAvgPrice*monthsLeft/12)
						line.setAmount(back*(-1))
						line.itemId=mol.getItemId()
						line.useItem = false
						line.description=mol.getDescription()
						//println line.toString()
		
						// add line to order
						
						def lines = order.orderLines as List
						lines.add(line)
						order.orderLines = lines.toArray()
		
						
						
						
						//editNewOrderLine();
						//addOrderLineToNew();
						
						/*if(hasPriceChanged(mol.getPriceAsDecimal(), totalQuantity,payPlan)){
							
									
							
						}
						else{
						
							//changeQuantityMasterLine();
						}*/
					}
					else{
						//addOrderLineToMaster();
					}
				}
				
				
			}
			//webServicesSession.updateOrder(masterOrder)
		}
		System.out.println("Order submethod "+order);
		
		
		// set success message in session, contents of the flash scope doesn't survive
		// the redirect to the order list when the web-flow finishes
		//session.message = 'order.created'
		//session.args = [ order.id, order.userId ]
		
		
	}
	
	//Checks if the new price is going to be different to the old one
	def boolean hasPriceChanged(oldPrice, totalQuantity,payPlan){
		def hasChanged=false
		
		BigDecimal newPrice = checkPrice(totalQuantity, payPlan);
		int np=newPrice.intValue()
		int op=oldPrice.intValue()
		
		println "NP"+np
		println "OP"+op
		if(op!=np){
			hasChanged=true
		}
		return hasChanged;
	}
	
	//Reads the payPlan .ods file containing the prices depending on users. Returns the price per user
	def BigDecimal checkPrice(quantity,payPlan){
		int q=quantity.intValue()
		def file = new File("resources/pay_plans/${payPlan}.ods");
		def sheet = SpreadSheet.createFromFile(file).getSheet(0);
		
		return sheet.getCellAt("B${q}").getValue();
	}
}
