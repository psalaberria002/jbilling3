

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

import java.math.RoundingMode

import org.apache.poi.common.usermodel.LineStyle;
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.jopendocument.dom.spreadsheet.SpreadSheet

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.order.OrderBL
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.Constants



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
				
				//Add double linked items to the linesToAdd ArrayList
				def doubleLinkedChildren=new ArrayList<Integer>();
				def linesToAdd=new ArrayList<OrderLineWS>();
				
					doubleLinkedChildren=webServicesSession.getDoubleLinkedChildren(line.itemId)
					if(!doubleLinkedChildren.isEmpty()){
						for(Integer itemId: doubleLinkedChildren){
							// build line
							def l = new OrderLineWS()
							l.typeId = Constants.ORDER_LINE_TYPE_ITEM
							l.quantity = line.getQuantityAsDecimal()
							l.itemId = itemId
							l.useItem = true
							
							linesToAdd.add(l)
						}
					}
					
				// add line to order
				def order = conversation.order
				def lines = order.orderLines as List
				lines.add(line)
				//add double linked item lines to order
				if(!linesToAdd.isEmpty()){
					for(OrderLineWS oline: linesToAdd){
						lines.add(oline)
					}
				}
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
				
				//update double linked items in order
				def doubleLinkedChildren=webServicesSession.getDoubleLinkedChildren(line.itemId)
				//def linesToChange=new ArrayList<OrderLineWS>();
					if(!doubleLinkedChildren.isEmpty()){
						def lines = order.orderLines as List
						int cont=0;
						for(OrderLineWS li:lines){
							cont+=1;
							for(Integer itemId: doubleLinkedChildren){
								if(itemId.equals(li.getItemId())){
									// edit line quantity
									li.quantity = line.getQuantityAsDecimal()
								}
								
							}
						}
						
					}
				
                // rate order
                if (order.orderLines) {
					//println order+" order"
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
			

            //on("save").to("saveOrder")
            // on("save").to("checkItem")  // check to see if an item exists, and show an information page before saving
            on("save").to("beforeSave") // show an information page before saving
            //on("cancel").to("finish")
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
		beforeSave{
			action{
				def order=conversation.order
				println order.toString()
				def dependencyMap=webServicesSession.checkDependencies(order)
				if(dependencyMap.isEmpty()){
					def masterOrder = webServicesSession.getMasterOrder(order.userId)
					def newOrder
					if(masterOrder==null){
						
						if(order.isMaster==1){
							//order=addDoubleLinkedItems(order)
							newOrder=moveOneTimersToNewOrder(order)
							save()
						}
						else if(order.addToMaster==1){
								cancel()
						}
						//Normal order
						else{
							save()
						}
					}
					else{
						if(order.isMaster==1){
							cancel()
						}
						else if(order.addToMaster==1){
							//order=addDoubleLinkedItems(order)
							newOrder=moveOneTimersToNewOrder(order)
							save()
						}
						else{
							save()
						}
					}
				
				}
				//Some dependencies yet.
				else{
						ArrayList<String> messages=new ArrayList<String>();
						String message;
						for (Map.Entry<Integer, Integer> entry : dependencyMap.entrySet())
						{
							message="You have to buy "+entry.getValue()+" more "+entry.getKey();
							messages.add(message);
						}
						//When the arraylist contains just one message, create an empty message to interpret it as arraylist in the view
						if(messages.size()==1){
							messages.add(0,"");
						}
						params.dependencies=messages//Add dependencies to the params.
						
						cancel();//Go to build. Review template will show the dependencies.
				}
				
			}
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
								
								//Save the order. Not more dependencies.
								
									//println "create new order, not to master"
									log.debug("creating order ${order}")
									order.id = webServicesSession.createOrder(order)
									
									//webServicesSession.updateOrder(order) //updates the order, adding a row into purchase_order_master
									
									// set success message in session, contents of the flash scope doesn't survive
									// the redirect to the order list when the web-flow finishes
									session.message = 'order.created'
									session.args = [ order.id, order.userId ]
								}
								//Creating and adding a new order to the master order
								else {
									//println "editOrders!!!"
									log.debug("creating edited order ${order}")
									def masterOrder = webServicesSession.getMasterOrder(order.userId)
									
									//Edit master order and new order
									masterOrder=editOrders(order,masterOrder);
									
									//println "pasa"
	
									order.orderLines = order.orderLines.sort { it.itemId }
								
									order.id = webServicesSession.createUpdateOrder(order)
									
									
									OrderBL obl = new OrderBL(order.id);
									order=obl.getWS(session['language_id']);
									//webServicesSession.updateOrder(order) //updates the order, adding a row into purchase_order_master
	
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
							//println "Editing"
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
			on("update").to("build")
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
	def  editOrders(order, masterOrder){
		//monthsLeft
		//Getting the starting day of the new order
		def orderActiveDay = order.getActiveSince()
		Calendar calActive = Calendar.getInstance();
		calActive.setTime(orderActiveDay);
		def newOrderActiveSinceYear= calActive.get(Calendar.YEAR);
		def monthStart = calActive.get(Calendar.MONTH);
		def dayStart = calActive.get(Calendar.DAY_OF_MONTH);
		System.out.println(newOrderActiveSinceYear+" "+monthStart+" "+dayStart);
		//Getting next billable day of the master order
		def masterOrderNextBillableDay = masterOrder.getNextBillableDay()
		Calendar cal=Calendar.getInstance();
		cal.setTime(masterOrderNextBillableDay);
		def masterOrderNextBillableYear= cal.get(Calendar.YEAR);
		def masterYear= masterOrderNextBillableYear-1;
		def monthNext = cal.get(Calendar.MONTH);
		
		//Getting previous day of the master order next billable day
		cal.add(Calendar.DAY_OF_MONTH, -1);
		cal.add(Calendar.MONTH, 1)
		System.out.println(cal.getTime());
		def prevDayNext = cal.get(Calendar.DAY_OF_MONTH);
		def prevMonthNext = cal.get(Calendar.MONTH);
		def prevYearNext = cal.get(Calendar.YEAR);
		System.out.println(prevDayNext+" "+prevMonthNext+" "+prevYearNext);
		//Getting the next month of the activeSince for the new order (Since January is 0, it will take the value 1 instead)
		//It is just for creating the description (Period from mm/dd/yyyy to mm/dd/yyyy)
		calActive.add(Calendar.MONTH, 1);
		def monthStartPrint = calActive.get(Calendar.MONTH);
		System.out.println(monthStartPrint);
		//If the years are different
		if(newOrderActiveSinceYear!=masterOrderNextBillableYear){
			monthNext+=12;
		}
		
		
		//Months for the new order
		def monthsLeft=monthNext-monthStart
		
		def back=0
		def masterOrderLines = masterOrder.getOrderLines()
		def newOrderLines = order.getOrderLines()
		def nolQuantity=0
		def molQuantity=0
		def totalQuantity=0
		newOrderLines.each { nol -> 
			newOrderActiveSinceYear= calActive.get(Calendar.YEAR);
			monthStartPrint = calActive.get(Calendar.MONTH);
			dayStart = calActive.get(Calendar.DAY_OF_MONTH);
			prevDayNext = cal.get(Calendar.DAY_OF_MONTH);
			prevMonthNext = cal.get(Calendar.MONTH);
			prevYearNext = cal.get(Calendar.YEAR);
			totalQuantity=0
			boolean found=false
			nolQuantity=nol.getQuantityAsDecimal()
			masterOrderLines.each { mol ->
				newOrderActiveSinceYear= calActive.get(Calendar.YEAR);
				monthStartPrint = calActive.get(Calendar.MONTH);
				dayStart = calActive.get(Calendar.DAY_OF_MONTH);
				prevDayNext = cal.get(Calendar.DAY_OF_MONTH);
				prevMonthNext = cal.get(Calendar.MONTH);
				prevYearNext = cal.get(Calendar.YEAR);
				if(found==false){
					if (nol.getItemId()==mol.getItemId()){
						found=true;
						if((nol.getTypeId()!=(Constants.ORDER_LINE_TYPE_TAX))){
							molQuantity=mol.getQuantityAsDecimal()
							totalQuantity=molQuantity+nolQuantity
							BigDecimal molOldAvgPrice
							//println "BERDINTZA "+BigDecimal.ZERO.compareTo(totalQuantity)
							if((BigDecimal.ZERO.compareTo(totalQuantity) != 0)){
								def payPlan=masterOrder.getPayPlan()
								molOldAvgPrice = mol.getPriceAsDecimal()
								//println molOldAvgPrice+" masterOrderLineAvgPrice"
								//println molQuantity+" molQuantity"
								back=molOldAvgPrice*molQuantity*monthsLeft/12
								//println back
								//println "resources/pay_plans/${payPlan}${mol.description}.ods"
								def file = new File("resources/pay_plans/${payPlan}_${mol.description}.ods");
								//println file.toPath()
								def sheet = SpreadSheet.createFromFile(file).getSheet(""+masterYear)
								//Negative number change to positive
								boolean negative=false;
								if(totalQuantity.intValue()<0){
									negative=true;
									totalQuantity=totalQuantity.negate();
								}
								BigDecimal value=sheet.getCellAt("B${totalQuantity.intValue()}").getValue()
								BigDecimal amount=sheet.getCellAt("C${totalQuantity.intValue()}").getValue()
								
								
								if(negative==true){
									amount=amount.negate();
								}
								//println value+" "+totalQuantity+" "+amount
								BigDecimal avgPrice=(BigDecimal)(amount/totalQuantity)
								//println avgPrice+" avgPrice"
								
								//Edit master order line
								mol.setPrice(avgPrice)
								mol.setQuantityAsDecimal(totalQuantity)
								mol.setAmount(amount)
								mol.useItem = true
								
								//webServicesSession.updateOrderLine(mol) //update Master Order Line
								//println mol.getPrice()+" "+mol.getAmount()
								
								//Edit new order line
								//println "Order before"+order
								nol.quantity= totalQuantity
								nol.setAmount(amount*monthsLeft/12)
								nol.setPrice(amount*monthsLeft/12/totalQuantity)
								println monthStartPrint
								monthStartPrint = (monthStartPrint < 10 ? '0' : '') + monthStartPrint;
								dayStart = (dayStart < 10 ? "0" : "") + dayStart;
								prevMonthNext= (prevMonthNext < 10 ? "0" : "") + prevMonthNext;
								prevDayNext= (prevDayNext < 10 ? "0" : "") + prevDayNext;
								nol.description=mol.getDescription()+" Period from "+monthStartPrint+"/"+dayStart+"/"+newOrderActiveSinceYear+" to "+prevMonthNext+"/"+prevDayNext+"/"+prevYearNext
								nol.useItem = false
								
								// build line
								def line = new OrderLineWS()
								line.typeId = Constants.ORDER_LINE_TYPE_ITEM
								line.quantity = (-1)*molQuantity
								line.setPrice(molOldAvgPrice*monthsLeft/12)
								line.setAmount(back*(-1))
								line.itemId=mol.getItemId()
								line.useItem = false
								line.description=mol.getDescription()+" Period from "+monthStartPrint+"/"+dayStart+"/"+newOrderActiveSinceYear+" to "+prevMonthNext+"/"+prevDayNext+"/"+prevYearNext
								
				
								// add line to order
								
								def lines = order.orderLines as List
								lines.add(line)
								order.orderLines = lines.toArray()
							}
							//When quantity == 0 just add back order line and delete from master.
							else{
								
								molOldAvgPrice = mol.getPriceAsDecimal()
								//println molOldAvgPrice+" masterOrderLineAvgPrice"
								//println molQuantity+" molQuantity"
								back=molOldAvgPrice*molQuantity*monthsLeft/12
								//println back+" back"
								
								//Edit master order line
								mol.setQuantityAsDecimal(totalQuantity)
								mol.setAmount(new BigDecimal(0))
								mol.useItem = false
								mol.setDeleted(1)
								
								//println nol+" nol"
								
								//Edit new order line
								nol.setDeleted(1)
								
								//println nol+" nol"
								
								// build line
								def line = new OrderLineWS()
								line.typeId = Constants.ORDER_LINE_TYPE_ITEM
								line.quantity = (-1)*molQuantity
								line.setPrice(molOldAvgPrice*monthsLeft/12)
								line.setAmount(back*(-1))
								line.itemId=mol.getItemId()
								line.useItem = false
								monthStartPrint = (monthStartPrint < 10 ? "0" : "") + monthStartPrint;
								dayStart = (dayStart < 10 ? "0" : "") + dayStart;
								prevMonthNext= (prevMonthNext < 10 ? "0" : "") + prevMonthNext;
								prevDayNext= (prevDayNext < 10 ? "0" : "") + prevDayNext;
								line.description=mol.getDescription()+" Period from "+monthStartPrint+"/"+dayStart+"/"+newOrderActiveSinceYear+" to "+prevMonthNext+"/"+prevDayNext+"/"+prevYearNext
				
								// add line to order
								
								def lines = order.orderLines as List
								lines.remove(nol)
								lines.add(line)
								order.orderLines = lines.toArray()
								
							}
							
							
							
							
							
						}
						
						
		
						
					}
				}
				
				
			}
			if(found==false){
				
				
				// build line
				def nolclone = new OrderLineWS()
				nolclone.typeId = Constants.ORDER_LINE_TYPE_ITEM
				nolclone.quantity = nol.quantity
				nolclone.setPrice(nol.price)
				nolclone.setAmount(nol.amount)
				nolclone.itemId=nol.itemId
				nolclone.useItem = true
				nolclone.description=nol.description
				//println line.toString()
				
				def linesm = masterOrder.orderLines as List
				linesm.add(nolclone)
				masterOrder.orderLines = linesm.toArray()
				
				def payPlan=masterOrder.getPayPlan()
				def file = new File("resources/pay_plans/${payPlan}_${nol.description}.ods");
				def sheet = SpreadSheet.createFromFile(file).getSheet(""+masterYear)
				def q=nol.getQuantityAsDecimal()
				//println q+" q"
				BigDecimal value=sheet.getCellAt("B${q.intValue()}").getValue()
				BigDecimal amount=sheet.getCellAt("C${q.intValue()}").getValue()
				//println value+" value "+q+" q"+amount+" amount"
				
				
				//Edit new order line
				nol.setAmount(amount*monthsLeft/12)
				nol.setPrice(amount*monthsLeft/12/q)
				monthStartPrint = (monthStartPrint < 10 ? "0" : "")+ monthStartPrint;
				dayStart = (dayStart < 10 ? "0" : "") + dayStart;
				prevMonthNext= (prevMonthNext < 10 ? "0" : "") + prevMonthNext;
				prevDayNext= (prevDayNext < 10 ? "0" : "") + prevDayNext;
				nol.description=nol.getDescription()+" Period from "+monthStartPrint+"/"+dayStart+"/"+newOrderActiveSinceYear+" to "+prevMonthNext+"/"+prevDayNext+"/"+prevYearNext
				nol.useItem = false
				
			}
			
			
		}
		//println order+" order"
		//println masterOrder+" masterOrder"
		//Check if there is a non deleted master order line. In that case update the order
		def molines=masterOrder.getOrderLines()
		boolean hasNotDeleted=false;
		molines.each { finalMol ->
			//println finalMol.deleted+" deleted"
			if(finalMol.deleted==0 && hasNotDeleted==false){
				hasNotDeleted=true;
				masterOrder.orderLines = masterOrder.orderLines.sort { it.itemId }
				webServicesSession.updateOrder(masterOrder)
			}
			
		}
		if(hasNotDeleted==false){
			//println "MASTER with deleted lines"
			//Set the lines to not deleted for updating the table item_users
			molines.each { finalMol ->
				finalMol.setDeleted(0)
				
			}
			webServicesSession.updateOrder(masterOrder)
			//If all the lines are deleted, delete the master order
			webServicesSession.deleteOrder(masterOrder.getId())
			
			
		}
		
		return masterOrder;	
	}
	
	/**
	 * It adds to the order the double linked items for the given order lines.
	 * @param order
	 * @return
	 */
	def addDoubleLinkedItems(order){
		def doubleLinkedChildren=new ArrayList<Integer>();
		def linesToAdd=new ArrayList<OrderLineWS>();
		def newOrderLines = order.getOrderLines()
		newOrderLines.each{nol->
			doubleLinkedChildren=webServicesSession.getDoubleLinkedChildren(nol.getItemId())
			if(!doubleLinkedChildren.isEmpty()){
				for(Integer itemId: doubleLinkedChildren){
					// build line
					def line = new OrderLineWS()
					line.typeId = Constants.ORDER_LINE_TYPE_ITEM
					line.quantity = nol.getQuantityAsDecimal()
					line.itemId = itemId
					line.useItem = true
					
					linesToAdd.add(line)
				}
			}
		}
		if(!linesToAdd.isEmpty()){
			def olines = order.orderLines as List
			for(OrderLineWS oline: linesToAdd){
				olines.add(oline)
			}
			
			order.orderLines=olines.toArray()
		}
		
		return order;
	}
	
	/**
	 * It will remove one timer items from the given order, and it will create a new one time order with these. 
	 * @param order
	 * @return
	 */
	def moveOneTimersToNewOrder(order){
		OrderWS newOrder=null;
		def linesToDelete=null;
		def orderLines = order.getOrderLines()
		orderLines.each{ol->
			if(ol.getDeleted()==0){
				if(webServicesSession.isOneTimer(ol.getItemId())){
					if(newOrder==null){
						newOrder=new OrderWS();
						
						newOrder.userId        = order.getUserId()
						newOrder.currencyId    = order.getCurrencyId()
						newOrder.statusId      = Constants.ORDER_STATUS_ACTIVE
						newOrder.period        = Constants.ORDER_PERIOD_ONCE
						newOrder.billingTypeId = Constants.ORDER_BILLING_PRE_PAID
						newOrder.activeSince   = order.getActiveSince()
						newOrder.isCurrent     = 0
						newOrder.billingTypeStr= order.getBillingTypeStr()
						newOrder.orderLines    = []
						newOrder.payPlan       = order.getPayPlan()
						newOrder.isMaster      = 0
						newOrder.addToMaster   = 0
						
					}
					def lines = newOrder.orderLines as List
					// build line
					def olclone = new OrderLineWS()
					olclone.typeId = Constants.ORDER_LINE_TYPE_ITEM
					olclone.quantity = ol.quantity
					olclone.setPrice(ol.price)
					olclone.setAmount(ol.amount)
					olclone.itemId=ol.itemId
					olclone.useItem = ol.useItem
					olclone.description=ol.description
					//println line.toString()
					lines.add(olclone)
					newOrder.orderLines = lines.toArray()
					
					if(linesToDelete==null){
						linesToDelete= new ArrayList<OrderLineWS>()
					}
					linesToDelete.add(ol)
					//ol.setDeleted(1);//delete line from order
					
					
				}
			}
			
			
		}
		if(linesToDelete){
			def olines = order.orderLines as List
			for(OrderLineWS oline: linesToDelete){
				println oline
				olines.remove(oline)
			}
			
			order.orderLines=olines.toArray()
		}
		if(newOrder!=null){
			println newOrder.toString()
			def newOrderId = webServicesSession.createOrder(newOrder)
			println newOrderId+" newOrderId"
		}

		return order;
	}
	
}
