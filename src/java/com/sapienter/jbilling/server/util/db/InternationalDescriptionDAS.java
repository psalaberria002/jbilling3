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

import java.util.Collection;

import org.hibernate.Query;


/**
 * 
 * @author abimael
 *
 */
public class InternationalDescriptionDAS extends AbstractDAS<InternationalDescriptionDTO> {

    private JbillingTableDAS jtDAS; // injected by Spring

    // should only be created from Spring
    protected InternationalDescriptionDAS() {
        super();
    }

    public void setJbDAS(JbillingTableDAS util) {
        this.jtDAS = util;
    }

    public InternationalDescriptionDTO findIt(String table,
            Integer foreignId, String column, Integer language) {

        if (foreignId == null || foreignId == 0) {
            return null;
        }
        
        InternationalDescriptionId idi =
                new InternationalDescriptionId(jtDAS.findByName(table).getId(),
                (foreignId == null) ? 0 : foreignId, column, (language == null) ? 0 : language);

        return find(idi); // this should cache ok
    }

    public InternationalDescriptionDTO create(String table, Integer foreignId, String column,
            Integer language, String message) {

        InternationalDescriptionId idi = new InternationalDescriptionId(
                jtDAS.findByName(table).getId(), foreignId, column, language);

        InternationalDescriptionDTO inter = new InternationalDescriptionDTO();
        inter.setId(idi);
        inter.setContent(message);

        return save(inter);

    }

    public Collection<InternationalDescriptionDTO> findByTable_Row(String table, Integer foreignId) {
        final String QUERY = "SELECT a " +
            "FROM InternationalDescriptionDTO a, JbillingTable b " +
            "WHERE a.id.tableId = b.id " +
            "AND b.name = :table " +
            "AND a.id.foreignId = :foreing ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("table", table);
        query.setParameter("foreing", foreignId);
        return query.list();
    }

    public static InternationalDescriptionDAS getInstance() {
        return new InternationalDescriptionDAS();
    }
}
