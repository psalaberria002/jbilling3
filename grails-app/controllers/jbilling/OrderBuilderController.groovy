

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
import com.sapienter.jbilling.server.item.db.ItemDTO



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
					
					
					
				// add line to order if not already added. preventing double items
				def order = conversation.order
				def lines = order.orderLines as List
				boolean has=false;
				println lines.size()
				for(int i=0;i<lines.size();i++){
					OrderLineWS ol=lines.get(i);
					println "adding line "+ol.getItemId().equals(line.itemId)
					if(ol.getItemId().equals(line.itemId)){
						has=true;
					}
					
				}
				if(!has){
					lines.add(line);
				}
			
				//When addToMaster==1 automatically add already bought items to the order
				if(order.addToMaster==1 && !has){
					//Add already bought items to the linesToAdd ArrayList
					def itemUsersItems=new ArrayList<Integer>();
					itemUsersItems=webServicesSession.getItemUsersItems(order.userId)
						if(!itemUsersItems.isEmpty()){
							for(Integer itemId: itemUsersItems){
								//Non installation fees
								if(!(webServicesSession.isOneTimer(itemId)&&webServicesSession.hasToBeQuantityOne(itemId).equals(1))){
									boolean contains=false;
									Iterator<OrderLineWS> ite=linesToAdd.iterator();
									while(ite.hasNext()&& !contains){
										OrderLineWS ol=ite.next();
										if(ol.getItemId().equals(itemId)){
											contains=true;
										}
									}
									lines.each{
										if(it.getItemId().equals(itemId)){
											contains=true;
										}
									}
									if(!contains && !line.itemId.equals(itemId)){
										println "contains"
										// build line
										def l = new OrderLineWS()
										l.typeId = Constants.ORDER_LINE_TYPE_ITEM
										l.quantity = line.getQuantityAsDecimal()
										l.itemId = itemId
										l.useItem = true
										linesToAdd.add(l)
									}
								}
								
								
							}
						}
				}
				
				
				//add double linked items and already bought items to order
				
				if(!linesToAdd.isEmpty()){
					println linesToAdd.size()
					for(int i=0;i<linesToAdd.size();i++){
						println lines.size()
						has=false;
						OrderLineWS oline=linesToAdd.get(i);
						for(int j=0;j<lines.size();j++){
							OrderLineWS ol=lines.get(j);
							if(ol.getItemId().equals(oline.getItemId())){
								has=true;
								break;
							}
						}
						if(!has){
							lines.add(oline);
						}
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

                params.newLineIndex = lines.size() - linesToAdd.size() -1
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
				
				// add line to order
				def lines = order.orderLines as List
				def linesToAdd=new ArrayList<OrderLineWS>();
				//When addToMaster==1 automatically add already bought items to the order
				if(order.addToMaster==1){
					//Add already bought items to the linesToAdd ArrayList
					def itemUsersItems=new ArrayList<Integer>();
					itemUsersItems=webServicesSession.getItemUsersItems(order.userId)
						if(!itemUsersItems.isEmpty()){
							for(Integer itemId: itemUsersItems){
								//Non installation fees
								if(!(webServicesSession.isOneTimer(itemId)&&webServicesSession.hasToBeQuantityOne(itemId).equals(1))){
									boolean contains=false;
									Iterator<OrderLineWS> ite=linesToAdd.iterator();
									while(ite.hasNext()&& !contains){
										OrderLineWS ol=ite.next();
										if(ol.getItemId().equals(itemId)){
											contains=true;
										}
									}
									lines.each{
										if(it.getItemId().equals(itemId)){
											contains=true;
										}
									}
									if(!contains && !line.itemId.equals(itemId)){
										println "contains"
										// build line
										def l = new OrderLineWS()
										l.typeId = Constants.ORDER_LINE_TYPE_ITEM
										l.quantity = line.getQuantityAsDecimal()
										l.itemId = itemId
										l.useItem = true
										linesToAdd.add(l)
									}
								}
								
								
							}
						}
				}
				
				
					//add double linked items and already bought items to order
					if(!linesToAdd.isEmpty()){
						for(OrderLineWS oline: linesToAdd){
							println oline.getDescription()
							lines.add(oline)
						}
					}
					
				order.orderLines = lines.toArray()
				
				lines = order.orderLines as List
				def checked=new ArrayList<Integer>();
				def visited=webServicesSession.bfsRelatedItemsInOrder(line,lines)
				lines=webServicesSession.updateQuantityInOrderLines(lines,visited,line.getQuantityAsDecimal())
				
				order.orderLines=lines;
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
				
				

                // one time orders are ALWAYS pre-paid
                if (order.period == Constants.ORDER_PERIOD_ONCE)
                    order.billingTypeId = Constants.ORDER_BILLING_PRE_PAID
				
					

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
				def dependencyMap=new HashMap<Integer,Integer>();
				//def minItemsMap=new HashMap<Integer,Integer>();
				//Check dependencies just when creating, not when editing. 
				if (!order.id || order.id == 0) {
                        if (SpringSecurityUtils.ifAllGranted("ORDER_20"))  {
							dependencyMap=webServicesSession.checkDependencies(order)
							//minItemsMap=webServicesSession.getMinItemsMap(order)
							//dependencyMap=webServicesSession.mergeMapsWithMaxValue(dependencyMap,minItemsMap)
						} 
						else{
							redirect controller: 'login', action: 'denied'
						}
				}
				
				if(dependencyMap.isEmpty()){
					def masterOrder = webServicesSession.getMasterOrder(order.userId)
					if(masterOrder==null){
						
						if(order.isMaster==1){
							//order=webServicesSession.addDoubleLinkedItems(order)
							order=webServicesSession.moveOneTimersToNewOrder(order)
							conversation.order=order
							println "dependency map empty, masterOrder=null and isMaster=1 "+order
							save()
						}
						else if(order.addToMaster==1){
							ArrayList<String> messages=new ArrayList<String>();
							String message;
							message="Not able to add this order to the master order. Create a master order first!";
							messages.add(message);
							//When the arraylist contains just one message, create an empty message to interpret it as arraylist in the view
							if(messages.size()==1){
								messages.add(0,"");
							}
							params.dependencies=messages
							cancel()
						}
						//Normal order
						else{
							save()
						}
					}
					else{
						if(order.isMaster==1){
							if(order.getId().equals(null)||order.getId().equals(0)){
								ArrayList<String> messages=new ArrayList<String>();
								String message;
								message="You cannot create more than one master order for this user. The master order for "+order.getUserId()+" is already created!";
								messages.add(message);
								//When the arraylist contains just one message, create an empty message to interpret it as arraylist in the view
								if(messages.size()==1){
									messages.add(0,"");
								}
								params.dependencies=messages
								cancel()
							}
							else{
								save()
							}		
						}
						else if(order.addToMaster==1){
							//order=webServicesSession.addDoubleLinkedItems(order)
							//order=webServicesSession.moveOneTimersToNewOrder(order)
							//conversation.order=order
							println "dependency map empty, masterOrder exists and addToMaster=1 "+order
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
							def neededItem = ItemDTO.findById(entry.getKey())
							message="You have to buy "+entry.getValue()+" more "+neededItem.getDescription();
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
					println "Save order"
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
									//println "before"
									//Edit master order and new order
									order=webServicesSession.editOrders(order,masterOrder);
									
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
	
}
