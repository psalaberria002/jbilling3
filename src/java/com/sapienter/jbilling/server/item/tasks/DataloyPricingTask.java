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
    

    public BigDecimal getPrice(Integer itemId, BigDecimal quantity, Integer userId, Integer currencyId,
            List<PricingField> fields, BigDecimal defaultPrice, OrderDTO pricingOrder, boolean singlePurchase)
            throws TaskException {
    	//System.out.println(currencyId+" cId");
        // now we have the line with good defaults, the order and the item
        // These have to be visible to the rules
        /*KnowledgeBase knowledgeBase;
        try {
            knowledgeBase = readKnowledgeBase();
        } catch (Exception e) {
            throw new TaskException(e);
        }
        StatelessKnowledgeSession mySession = knowledgeBase.newStatelessKnowledgeSession();
        List<Object> rulesMemoryContext = new ArrayList<Object>();*/
        
        PricingManager manager = new PricingManager(itemId, userId, currencyId, defaultPrice);
        //mySession.setGlobal("manager", manager);
        
        //System.out.println(pricingOrder.toString());
        ItemBL ibl=new ItemBL(itemId);
        
        ItemDTO idto=ibl.getEntity();
        String description=idto.getDescription();
        String payPlan=pricingOrder.getPayPlan();
        int year=0;
        
        
        Calendar cal = Calendar.getInstance();
        OrderDTO masterOrder=null;
        //The order is going to be added to the master order
        if(pricingOrder.getAddToMaster()==1){
        	//Getting master order
        	
            OrderDAS das=new OrderDAS();
            masterOrder = das.findMasterByUser(userId);
            System.out.println(masterOrder.getId());
            Date masterOrderNextBillableDay = masterOrder.getNextBillableDay();
            //If invoice never generated for the master-> masterOrderNextBillable=null
            cal.setTime(masterOrderNextBillableDay);
            year=cal.get(Calendar.YEAR)-1;
            System.out.println(year+" "+masterOrderNextBillableDay);
        }
        else if(pricingOrder.getIsMaster()==1){
        	masterOrder=pricingOrder;
        	Date masterOrderNextBillableDay = masterOrder.getNextBillableDay();
        	//System.out.println(masterOrderNextBillableDay+" billableday");
        	//When creating
        	if(masterOrderNextBillableDay==null){
        		cal.setTime(masterOrder.getActiveSince());
                year=cal.get(Calendar.YEAR);
        	}
        	else{
        		cal.setTime(masterOrderNextBillableDay);
        		//New price if the next billableDay is in less than 25 days from the current date
        		Calendar calNow =Calendar.getInstance();
        		//System.out.println("Barruan!");
        		long milliseconds1 = calNow.getTimeInMillis();
        	    long milliseconds2 = cal.getTimeInMillis();
        	    long diff = milliseconds2 - milliseconds1;
        	    long diffDays = diff / (24 * 60 * 60 * 1000);
        	    //System.out.println(diffDays+" diffDays"); 
        	    //Change the price for the master order
        		if(diffDays<=25){
        			year=cal.get(Calendar.YEAR);
        		}
        		//Editing master order during the year with master price (11months and first days of the 12th included)
        		else{
                    year=cal.get(Calendar.YEAR)-1;
        		}
        		
        	}
        	
        }
        //Normal order
        else{
            cal.setTime(pricingOrder.getActiveSince());
            year=cal.get(Calendar.YEAR);
        }
      
      		
        //System.out.println(year+" year");
        
        BigDecimal avgPrice=defaultPrice;
		if(payPlan!=null){
			try {
				
				File file = new File("resources/pay_plans/"+payPlan+"_"+description+".ods");
				//System.out.println(file.toPath());
				Sheet sheet=null;
				//System.out.println("Aure");
				sheet = SpreadSheet.createFromFile(file).getSheet(""+year);
				//System.out.println("gero");
				//Negative number change to positive
				boolean negative=false;
				if(quantity.intValue()<0){
					negative=true;
					quantity=quantity.negate();
				}
				BigDecimal value=(BigDecimal) sheet.getCellAt("B"+quantity.intValue()).getValue();
				BigDecimal amount=(BigDecimal) sheet.getCellAt("C"+quantity.intValue()).getValue();
				avgPrice=amount.divide(quantity, 10, RoundingMode.HALF_EVEN);
				if(negative==true){
					amount=amount.negate();
				}
				//System.out.println(value+" "+amount+" "+avgPrice);
				
			} catch (IOException e) {
				//System.out.println("Error reading from file");
				e.printStackTrace();
			}
			
		}
		
		//If the currency is NOT Norwegian Krone
		if(!pricingOrder.getCurrency().getCode().equals("NOK")){
			CurrencyBL cbl=new CurrencyBL();
			//System.out.println(idto.getCurrencyId()+currencyId.toString()+avgPrice+pricingOrder.getUser().getEntity().getId());
			Iterator<ItemPriceDTO> it=idto.getItemPrices().iterator();
			boolean go=false;
			Integer fromCurrency=0;
			while(it.hasNext() && go==false){
				ItemPriceDTO ip=it.next();
				//CurrencyId for NOK
				if(ip.getCurrency().getCode().equals("NOK")){
					go=true;
					fromCurrency=ip.getCurrencyId();
				}
			}
			avgPrice=cbl.convert(fromCurrency, currencyId, avgPrice, pricingOrder.getUser().getEntity().getId());
		}
		
		
		manager.setPrice(avgPrice);
		
		
		
		
		/*mySession.execute(rulesMemoryContext);*/

        return manager.getPrice();
    }
}
