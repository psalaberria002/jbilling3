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
package com.sapienter.jbilling.server.user.contact.db;

import org.hibernate.Query;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class ContactFieldDAS extends AbstractDAS<ContactFieldDTO> {
    private static final String findByTypeSQL = 
        "SELECT a " +
        "  FROM ContactFieldDTO a " +
        " WHERE a.contact.id = :contactId " +
        "   AND a.type.id = :typeId ";
               
    public ContactFieldDTO findByType(Integer typeId, Integer contactId) {
        Query query = getSession().createQuery(findByTypeSQL);
        query.setParameter("contactId", contactId);
        query.setParameter("typeId", typeId);
        return (ContactFieldDTO) query.uniqueResult();
    }

}
