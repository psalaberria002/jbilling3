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
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.order.db.OrderDTO;


public class CurrencyExchangeDAS extends AbstractDAS<CurrencyExchangeDTO> {
    private static final String findExchangeSQL =
        "SELECT a " +
        "  FROM CurrencyExchangeDTO a " +
        " WHERE a.entityId = :entity " +
        "   AND a.currency.id = :currency";
    
    private static final String  findByEntitySQL =
        " SELECT a " +
        "   FROM CurrencyExchangeDTO a " +
        "  WHERE a.entityId = :entity";
    
    
    public int updateExchangeRateById(int id,BigDecimal rate){
   		 
   		 Query query = getSession().createSQLQuery(
 	    			"UPDATE currency_exchange SET rate=:rate WHERE id=:id ")
 	    			.setParameter("id", id)
 	    			.setParameter("rate", rate);
   		 
   		 return query.executeUpdate();
    }
    
    public Integer findExchangeId(int entityId, Integer cid) {
    	Object result = (Object) getSession()
                .createSQLQuery("select id from currency_exchange where currency_id=:cid AND entity_id=:entityId")
                .setParameter("cid", cid)
                .setParameter("entityId", entityId)
                .uniqueResult();
   	 
        
        return (Integer)result;
	}

    public CurrencyExchangeDTO findExchange(Integer entityId,Integer currencyId) {
        Query query = getSession().createQuery(findExchangeSQL);
        query.setParameter("entity", entityId);
        query.setParameter("currency", currencyId);
        return (CurrencyExchangeDTO) query.uniqueResult();
    }
    
    public List<CurrencyExchangeDTO> findByEntity(Integer entityId) {
        Query query = getSession().createQuery(findByEntitySQL);
        query.setParameter("entity", entityId);
        return query.list();
    }


	
}
