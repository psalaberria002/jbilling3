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
package com.sapienter.jbilling.server.item.tasks;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemPriceDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;

public class DataloyPricingTask extends PluggableTask implements IPricing {
    
    private static final Logger LOG = Logger.getLogger(RulesPricingTask.class);
    
    /**
     * Get the price for the given item, user, and quantity being purchased. Prices are going to be gathered from the correspondent .ods files.
     *
     * @param itemId item id being purchased
     * @param quantity quantity being purchased 
     * @param userId user purchasing the item
     * @param currencyId currency of user
     * @param fields pricing fields
     * @param defaultPrice default price if no other price could be determined.
     * @param pricingOrder target order for this pricing request (may be null)
     * @param singlePurchase true if pricing a single purchase/addition to an order, false if pricing a quantity that already exists on the given order.
     * @return price
     * @throws TaskException checked exception if a problem occurs.
     */
    public BigDecimal getPrice(Integer itemId, BigDecimal quantity, Integer userId, Integer currencyId,
            List<PricingField> fields, BigDecimal defaultPrice, OrderDTO pricingOrder, boolean singlePurchase)
            throws TaskException {
        
        PricingManager manager = new PricingManager(itemId, userId, currencyId, defaultPrice);
        
        ItemBL ibl=new ItemBL(itemId);
        ItemDTO idto=ibl.getEntity();
        String description=idto.getDescription();
        String payPlan=pricingOrder.getPayPlan();
        int year=0;
        Calendar cal = Calendar.getInstance();
        OrderDTO masterOrder=null;
        
        // Select year for pricing
        // the order is going to be added to the master order
        if(pricingOrder.getAddToMaster()==1){
        	// getting master order
            OrderDAS das=new OrderDAS();
            masterOrder = das.findMasterByUser(userId);
            Date masterOrderNextBillableDay = masterOrder.getNextBillableDay();
            // if invoice never generated for the master-> masterOrderNextBillableDay=null
            if(masterOrderNextBillableDay==null){
            	LOG.debug("Master order hasn't been invoiced yet! masterOrderNextBillableDay==null");
            }
            cal.setTime(masterOrderNextBillableDay);
            year=cal.get(Calendar.YEAR)-1;
        }
        // master order
        else if(pricingOrder.getIsMaster()==1){
        	masterOrder=pricingOrder;
        	Date masterOrderNextBillableDay = masterOrder.getNextBillableDay();
        	// when creating
        	if(masterOrderNextBillableDay==null){
        		cal.setTime(masterOrder.getActiveSince());
                year=cal.get(Calendar.YEAR);
        	}
        	// when already invoiced at least once
        	else{
        		cal.setTime(masterOrderNextBillableDay);	
        		Calendar calNow =Calendar.getInstance();
        		long milliseconds1 = calNow.getTimeInMillis();
        	    long milliseconds2 = cal.getTimeInMillis();
        	    long diff = milliseconds2 - milliseconds1;
        	    long diffDays = diff / (24 * 60 * 60 * 1000);
        	    // new price if the current date is within the last 35 days for the next billable day.
        		if(diffDays<=35){
        			year=cal.get(Calendar.YEAR);
        		}
        		// master price. Editing master order during the year with master price (excluding last month of the master, and some days of the 11th month)
        		else{
                    year=cal.get(Calendar.YEAR)-1;
        		}
        		
        	}
        	
        }
        // normal order
        else{
            cal.setTime(pricingOrder.getActiveSince());
            year=cal.get(Calendar.YEAR);
        }
      
        // reading price from file
        BigDecimal avgPrice=defaultPrice;
		if(payPlan!=null){
			try {
				
				File file = new File("resources/pay_plans/"+payPlan+"_"+description+".ods");
				Sheet sheet=null;
				sheet = SpreadSheet.createFromFile(file).getSheet(""+year);
				
				// negative number change to positive
				boolean negative=false;
				if(quantity.intValue()<0){
					negative=true;
					quantity=quantity.negate();
				}
				
				//BigDecimal value=(BigDecimal) sheet.getCellAt("B"+quantity.intValue()).getValue();
				BigDecimal amount=(BigDecimal) sheet.getCellAt("C"+quantity.intValue()).getValue();
				avgPrice=amount.divide(quantity, 10, RoundingMode.HALF_EVEN);
				
				// back to negative
				if(negative==true){
					amount=amount.negate();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// convert price from NOK to currency if the currency is NOT Norwegian Krone
		if(!pricingOrder.getCurrency().getCode().equals("NOK")){
			CurrencyBL cbl=new CurrencyBL();
			Iterator<ItemPriceDTO> it=idto.getItemPrices().iterator();
			boolean go=false;
			Integer fromCurrency=0;
			while(it.hasNext() && go==false){
				ItemPriceDTO ip=it.next();
				if(ip.getCurrency().getCode().equals("NOK")){
					go=true;
					fromCurrency=ip.getCurrencyId();//CurrencyId for NOK
				}
			}
			avgPrice=cbl.convert(fromCurrency, currencyId, avgPrice, pricingOrder.getUser().getEntity().getId());
		}
		
		
		manager.setPrice(avgPrice);

        return manager.getPrice();
    }
}
