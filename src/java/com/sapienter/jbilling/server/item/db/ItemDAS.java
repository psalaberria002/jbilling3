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

import com.sapienter.jbilling.server.util.db.AbstractDAS;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

public class ItemDAS extends AbstractDAS<ItemDTO> {

    /**
     * Returns a list of all items for the given item type (category) id.
     * If no results are found an empty list will be returned.
     *
     * @param itemTypeId item type id
     * @return list of items, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<ItemDTO> findAllByItemType(Integer itemTypeId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("itemTypes", "type")
                .add(Restrictions.eq("type.id", itemTypeId));

        return criteria.list();
    }

    /**
     * Returns a list of all items with item type (category) who's
     * description matches the given prefix.
     *
     * @param prefix prefix to check
     * @return list of items, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<ItemDTO> findItemsByCategoryPrefix(String prefix) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("itemTypes", "type")
                .add(Restrictions.like("type.description", prefix + "%"));

        return criteria.list();
    }    

    public List<ItemDTO> findItemsByInternalNumber(String internalNumber) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("internalNumber", internalNumber));

        return criteria.list();
    }
    
    @SuppressWarnings("unchecked")
	public List<Object[]> getAllDependencies(){
		
      	 Query query = getSession()
                   .createSQLQuery("select a.item_id,a.child_item_id from item_dependency a " +
                   		"ORDER BY a.item_id ASC");
      	 	
       	return query.list();
       }
	@SuppressWarnings("unchecked")
	public List<Integer> getParents(Integer childId){
		
      	 Query query = getSession()
                   .createSQLQuery("select a.item_id from item_dependency a " +
                   		"WHERE a.child_item_id=:childId ORDER BY a.item_id ASC")
                   		.setParameter("childId", childId);
      	 	
       	return query.list();
       }
	@SuppressWarnings("unchecked")
	public void setParent(Integer childId, Integer parentId){
		
      	 Query query = getSession()
                   .createSQLQuery("insert into item_dependency values (:parentId,:childId)")
                   	.setParameter("parentId", parentId)
                   		.setParameter("childId", childId);
      	 	query.executeUpdate();
       	
       }
	@SuppressWarnings("unchecked")
	public void removeAllParents(Integer childId){
		
      	 Query query = getSession()
                   .createSQLQuery("delete from item_dependency where child_item_id=:childId")
                   		.setParameter("childId", childId);
      	query.executeUpdate();
       	
       }
}
