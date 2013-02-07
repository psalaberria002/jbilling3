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
package com.sapienter.jbilling.server.item.db;

import java.util.List;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public class ItemTypeDAS extends AbstractDAS<ItemTypeDTO> {

    public static final String PLANS_INTERNAL_CATEGORY_NAME = "plans";

    /**
     * Returns true if the given item type ID is in use.
     *
     * @param typeId type id
     * @return true if in use, false if not
     */
    public boolean isInUse(Integer typeId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("id", typeId))
                .createAlias("items", "item")
                .add(Restrictions.eq("item.deleted", 0)) // item type contains non-deleted items
                .setProjection(Projections.count("item.id"));

        criteria.setComment("ItemTypeDTO.isInUse");

        return (criteria.uniqueResult() != null && ((Integer) criteria.uniqueResult()) > 0);
    }

    /**
     * Returns the category that has the specified description.
     * @param description Description.
     * @return The category that matches the description or null if no category was found.
     */
    public ItemTypeDTO findByDescription(Integer entityId, String description) {
        Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
        		.add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("description", description));

        return (ItemTypeDTO) criteria.uniqueResult();
    }

    /**
     * 	Find By entity
     * 
     * @param entityId
     * @return returns all the categories per given entity
     */
	public List<ItemTypeDTO> findByEntityId(Integer entityId) {
		
        Criteria criteria = getSession().createCriteria(ItemTypeDTO.class)
        		.add(Restrictions.eq("entity.id", entityId));
        return criteria.list();
	}
}
