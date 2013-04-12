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
	public List<Integer> getChildren(Integer parentId){
      	 Query query = getSession()
                   .createSQLQuery("select a.child_item_id from item_dependency a " +
                   		"WHERE a.item_id=:parentId ORDER BY a.item_id ASC")
                   		.setParameter("parentId", parentId);
      	 
      	 System.out.println(query);	
       	return query.list();
       }
	
	@SuppressWarnings("unchecked")
	public List<Integer> getDoubleLinkedChildren(Integer parentId) {
		Query query = getSession()
                .createSQLQuery("select a.child_item_id from item_dependency a " +
                		"WHERE a.item_id=:parentId AND double_linked=1 ORDER BY a.item_id ASC")
                		.setParameter("parentId", parentId);
   
    	return query.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<Integer> getDoubleLinkedParents(Integer childId) {
		Query query = getSession()
                .createSQLQuery("select a.item_id from item_dependency a " +
                		"WHERE a.child_item_id=:childId AND double_linked=1 ORDER BY a.item_id ASC")
                		.setParameter("childId", childId);
   
    	return query.list();
	}
	
	@SuppressWarnings("unchecked")
	public void setParent(Integer childId, Integer parentId,Integer isDoubleLinked){
		
      	 Query query = getSession()
                   .createSQLQuery("insert into item_dependency values (:parentId,:childId,:doubleLinked)")
                   	.setParameter("parentId", parentId)
                   		.setParameter("childId", childId)
                   		.setParameter("doubleLinked",isDoubleLinked);
      	 	query.executeUpdate();
       	
       }
	@SuppressWarnings("unchecked")
	public void removeAllParents(Integer childId){
		
      	 Query query = getSession()
                   .createSQLQuery("delete from item_dependency where child_item_id=:childId")
                   		.setParameter("childId", childId);
      	query.executeUpdate();
       	
       }
	
	@SuppressWarnings("unchecked")
	public String getItemPeriod(Integer itemId) {
		Query query = getSession()
                .createSQLQuery("select a.period from item_period a " +
                		"WHERE a.item_id=:itemId")
                		.setParameter("itemId", itemId);
   
    	return (String)query.uniqueResult();
	}

	public void setItemPeriod(Integer itemId, String period, Integer quantityToOne) {
		Object result = (Object) getSession()
                .createSQLQuery("select * from item_period where item_id=:itemId")
                .setParameter("itemId", itemId)
                .uniqueResult();
   	 	//System.out.println(result);
   	 	Query query=null; 
   	 	if(result!=null){
   	 		//System.out.println("notnull");
   	 		query = getSession().createSQLQuery(
 	    			"UPDATE item_period SET period=:period, quantity_invoice_one=:quantityToOne WHERE item_id=:itemId")
 	    			.setParameter("itemId", itemId)
 	    			.setParameter("period", period)
 	    			.setParameter("quantityToOne", quantityToOne);
   	 	}
   	 	else{
   	 		//System.out.println("yesnull");
   	 		query = getSession()
                .createSQLQuery("insert into item_period values (:itemId,:period,:quantityToOne)")
                	.setParameter("itemId", itemId)
                		.setParameter("period", period)
                		.setParameter("quantityToOne", quantityToOne);
   	 	
   	 	}
   	 
		
   	 	query.executeUpdate();
	}

	public void deleteItemDependencies(Integer itemId) {
		Query query = getSession()
                .createSQLQuery("delete from item_dependency where item_id=:itemId OR child_item_id=:itemId")
            	.setParameter("itemId", itemId);
		query.executeUpdate();
		
	}

	public void deleteItemPeriod(Integer itemId) {
		Query query = getSession()
                .createSQLQuery("delete from item_period where item_id=:itemId")
            	.setParameter("itemId", itemId);
		query.executeUpdate();
		
	}

	public Integer hasToBeQuantityOne(int itemId) {
		Query query = getSession()
                .createSQLQuery("select quantity_invoice_one from item_period where item_id=:itemId")
            	.setParameter("itemId", itemId);
		
		Integer result=(Integer)query.uniqueResult();
		if(result!=null){
			return result;
		}
		else{
			return new Integer(0);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public Integer getMinItems(Integer itemId){
      	 Query query = getSession()
                   .createSQLQuery("select a.min_items from item_dependency a " +
                   		"WHERE a.item_id=:itemId AND a.child_item_id=:itemId")
                   		.setParameter("itemId", itemId);
      	 
      	Integer result=(Integer)query.uniqueResult();
		if(result!=null){
			return result;
		}
		else{
			return new Integer(0);
		}
       }

	public void setMinItems(Integer itemId, Integer minItems) {
		Object result = (Object) getSession()
                .createSQLQuery("select * from item_dependency where item_id=:itemId AND child_item_id=:itemId")
                .setParameter("itemId", itemId)
                .uniqueResult();
   	 	//System.out.println(result);
   	 	Query query=null; 
   	 	if(result!=null){
   	 		//System.out.println("notnull");
   	 		query = getSession().createSQLQuery(
 	    			"UPDATE item_dependency SET min_items=:minItems WHERE item_id=:itemId AND child_item_id=:itemId")
 	    			.setParameter("itemId", itemId)
 	    			.setParameter("minItems", minItems);
   	 	}
   	 	else{
   	 		//System.out.println("yesnull");
   	 		query = getSession()
                .createSQLQuery("insert into item_dependency values (:itemId,:itemId,0,:minItems)")
                	.setParameter("itemId", itemId)
                		.setParameter("minItems", minItems);
   	 	
   	 	}
   	 	query.executeUpdate();
	}
	
	
}
