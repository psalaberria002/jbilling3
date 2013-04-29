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

package com.sapienter.jbilling.server.util.db;

import java.math.BigDecimal;



public class CurrencyDAS extends AbstractDAS<CurrencyDTO> {

	public Integer findIdByCode(String code) {
    	
    	Object result = (Object) getSession()
                .createSQLQuery("select id from currency where code=:code")
                .setParameter("code", code)
                .uniqueResult();
   	 
        if(result instanceof BigDecimal){
        	return Integer.valueOf(((BigDecimal)result).intValue());
        }
        else{
        	return (Integer)result;
        }
        
    }
    
}
