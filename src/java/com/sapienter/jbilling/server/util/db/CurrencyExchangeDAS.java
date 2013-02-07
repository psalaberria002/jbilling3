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

import java.util.List;

import org.hibernate.Query;


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
