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
package com.sapienter.jbilling.server.order.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderDAS extends AbstractDAS<OrderDTO> {

    /**
     * Returns the newest active order for the given user id and period.
     *
     * @param userId user id
     * @param period period
     * @return newest active order for user and period.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findByUserAndPeriod(Integer userId, OrderPeriodDTO period) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.id", Constants.ORDER_STATUS_ACTIVE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.eq("orderPeriod", period))
                .addOrder(Order.asc("id"))
                .setMaxResults(1);

        return findFirst(criteria);
    }
    
    /**
     * Returns the newest active order for the given user id and period.
     *
     * @param userId user id
     * @param period period
     * @return newest active order for user and period.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findMasterByUser(Integer userId) {
    	Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isMaster", 1))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId));

        return findFirst(criteria);
    }
    

    public OrderProcessDTO findProcessByEndDate(Integer id, Date myDate) {
        return (OrderProcessDTO) getSession().createFilter(find(id).getOrderProcesses(), 
                "where this.periodEnd = :endDate").setDate("endDate", 
                        Util.truncateDate(myDate)).uniqueResult();
        
    }

    /**
     * Finds active recurring orders for a given user
     * @param userId
     * @return
     */
    public List<OrderDTO> findByUserSubscriptions(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                    .add(Restrictions.eq("s.id", Constants.ORDER_STATUS_ACTIVE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                    .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE));
        
        return criteria.list();
    }
    
    /**
     * Finds all active orders for a given user
     * @param userId
     * @return
     */
    public Object findEarliestActiveOrder(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                    .add(Restrictions.eq("s.id", Constants.ORDER_STATUS_ACTIVE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.asc("nextBillableDay"));

        return findFirst(criteria);
    }

    /**
     * Returns a scrollable result set of orders with a specific status belonging to a user.
     *
     * You MUST close the result set after iterating through the results to close the database
     * connection and discard the cursor!
     *
     * <code>
     *     ScrollableResults orders = new OrderDAS().findByUser_Status(123, 1);
     *     // do something
     *     orders.close();
     * </code>
     *
     * @param userId user ID
     * @param statusId order status to include
     * @return scrollable results for found orders.
     */
    public ScrollableResults findByUser_Status(Integer userId,Integer statusId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .createAlias("orderStatus", "s")
                    .add(Restrictions.eq("s.id", statusId));
        
        return criteria.scroll();
    }

    // used for the web services call to get the latest X orders
    public List<Integer> findIdsByUserLatestFirst(Integer userId, int maxResults) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setMaxResults(maxResults)
                .setComment("findIdsByUserLatestFirst " + userId + " " + maxResults);
        return criteria.list();
    }

    // used for the web services call to get the latest X orders that contain an item of a type id
    @SuppressWarnings("unchecked")
    public List<Integer> findIdsByUserAndItemTypeLatestFirst(Integer userId, Integer itemTypeId, int maxResults) {
        // I'm a HQL guy, not Criteria
        String hql = 
            "select distinct(orderObj.id)" +
            " from OrderDTO orderObj" +
            " inner join orderObj.lines line" +
            " inner join line.item.itemTypes itemType" +
            " where itemType.id = :typeId" +
            "   and orderObj.baseUserByUserId.id = :userId" +
            "   and orderObj.deleted = 0" +
            " order by orderObj.id desc";
        List<Integer> data = getSession()
                                .createQuery(hql)
                                .setParameter("userId", userId)
                                .setParameter("typeId", itemTypeId)
                                .setMaxResults(maxResults)
                                .list();
        return data;
    }

    /**
     * @author othman
     * @return list of active orders
     */
    public List<OrderDTO> findToActivateOrders() {
        Date today = Util.truncateDate(new Date());
        Criteria criteria = getSession().createCriteria(OrderDTO.class);

        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.or(Expression.le("activeSince", today),
                Expression.isNull("activeSince")));
        criteria.add(Restrictions.or(Expression.gt("activeUntil", today),
                Expression.isNull("activeUntil")));

        return criteria.list();
    }

    /**
     * @author othman
     * @return list of inactive orders
     */
    public List<OrderDTO> findToDeActiveOrders() {
        Date today = Util.truncateDate(new Date());
        Criteria criteria = getSession().createCriteria(OrderDTO.class);

        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.or(Expression.gt("activeSince", today),
                Expression.le("activeUntil", today)));

        return criteria.list();
    }
    
    public BigDecimal findIsUserSubscribedTo(Integer userId, Integer itemId) {
        String hql = 
                "select sum(l.quantity) " +
                "from OrderDTO o " +
                "inner join o.lines l " +
                "where l.item.id = :itemId and " +
                "o.baseUserByUserId.id = :userId and " +
                "o.orderPeriod.id != :periodVal and " +
                "o.orderStatus.id = :status and " +
                "o.deleted = 0 and " +
                "l.deleted = 0";

        BigDecimal result = (BigDecimal) getSession()
                .createQuery(hql)
                .setInteger("userId", userId)
                .setInteger("itemId", itemId)
                .setInteger("periodVal", Constants.ORDER_PERIOD_ONCE)
                .setInteger("status", Constants.ORDER_STATUS_ACTIVE)
                .uniqueResult();
        
        return (result == null ? BigDecimal.ZERO : result);
    }
    
    public Integer[] findUserItemsByCategory(Integer userId, 
            Integer categoryId) {
        
        Integer[] result = null;
        
        final String hql =
                "select distinct(i.id) " +
                "from OrderDTO o " +
                "inner join o.lines l " +
                "inner join l.item i " +
                "inner join i.itemTypes t " +
                "where t.id = :catId and " +
                "o.baseUserByUserId.id = :userId and " +
                "o.orderPeriod.id != :periodVal and " +
                "o.deleted = 0 and " +
                "l.deleted = 0";
        List qRes = getSession()
                .createQuery(hql)
                .setInteger("userId", userId)
                .setInteger("catId", categoryId)
                .setInteger("periodVal", Constants.ORDER_PERIOD_ONCE)
                .list();
        if (qRes != null && qRes.size() > 0) {
            result = (Integer[])qRes.toArray(new Integer[0]);
        }
        return result;
    }

    private static final String FIND_ONETIMERS_BY_DATE_HQL =
            "select o " +
                    "  from OrderDTO o " +
                    " where o.baseUserByUserId.id = :userId " +
                    "   and o.orderPeriod.id = :periodId " +
                    "   and cast(activeSince as date) = :activeSince " +
                    "   and deleted = 0";    

    @SuppressWarnings("unchecked")
    public List<OrderDTO> findOneTimersByDate(Integer userId, Date activeSince) {
        Query query = getSession().createQuery(FIND_ONETIMERS_BY_DATE_HQL)
                .setInteger("userId", userId)
                .setInteger("periodId", Constants.ORDER_PERIOD_ONCE)
                .setDate("activeSince", activeSince);

        return query.list();
    }

    public OrderDTO findForUpdate(Integer id) {
        OrderDTO retValue = super.findForUpdate(id);
        // lock all the lines
        for (OrderLineDTO line : retValue.getLines()) {
            new OrderLineDAS().findForUpdate(line.getId());
        }
        return retValue;
    }
    
    /**Changes the value of master, setting an order as Master or not. 
     * UPDATE OR INSERT
     * table purchase_order_master
     * 
     * @param orderId
     * @param b
     * @return
     */
    public int updateOrInsertOrderMaster(Integer orderId,Integer b){
    			
    	 Object result = (Object) getSession()
                 .createSQLQuery("select master from purchase_order_master where order_id=:orderId")
                 .setParameter("orderId", orderId)
                 .uniqueResult();
    	 //System.out.println(result);
    	 Query query=null; 
    	 if(result!=null){
    		 //System.out.println("notnull");
    		 query = getSession().createSQLQuery(
  	    			"UPDATE purchase_order_master SET master=:b WHERE order_id=:orderId")
  	    			.setParameter("orderId", orderId)
  	    			.setParameter("b", b);
    	 }
    	 else{
    		 //System.out.println("yesnull");
    		 query = getSession().createSQLQuery(
    	    			"INSERT INTO purchase_order_master VALUES ( :orderId, :b)")
    	    			.setParameter("orderId", orderId)
    	    			.setParameter("b", b);
    	 }
    	 
    	 
    	 return query.executeUpdate();
    }
    
    /**
     * Update or insert a row into item_users. 
     * @param itemId If itemId is an installation fee, then save 0 or 1.
     * @param userId
     * @param n
     * @return
     */
    public int updateOrInsertItemUsers(Integer itemId,Integer userId,Integer n){
    	
    	ItemDAS itemDas=new ItemDAS();
    	//If the item is an installation fee values will be 0 or 1 into item_users
		if(itemDas.hasToBeQuantityOne(itemId).equals(1)&&(itemDas.getItemPeriod(itemId).equals("One time")||itemDas.getItemPeriod(itemId).equals(null)||itemDas.getItemPeriod(itemId).equals(""))){
			if(n>0){
				n=new Integer(1);
			}
			else{
				n=new Integer(0);
			}
		}
		
		
    			
    	 Object result = (Object) getSession()
                 .createSQLQuery("select users from item_users where item_id=:itemId AND user_id=:userId")
                 .setParameter("itemId", itemId)
                 .setParameter("userId", userId)
                 .uniqueResult();
    	 Query query=null; 
    	 if(result!=null){
    		 query = getSession().createSQLQuery(
  	    			"UPDATE item_users SET users=:n WHERE item_id=:itemId AND user_id=:userId")
  	    			.setParameter("itemId", itemId)
  	    			.setParameter("userId", userId)
  	    			.setParameter("n", n);
    	 }
    	 else{
    		 query = getSession().createSQLQuery(
    	    			"INSERT INTO item_users VALUES ( :itemId, :userId , :n)")
    	    			.setParameter("itemId", itemId)
      	    			.setParameter("userId", userId)
      	    			.setParameter("n", n);
    	 }
    	 
    	 
    	 return query.executeUpdate();
    }
    
    /**
     * Returns the number of users for a given product and customer.
     * @param itemId
     * @param userId
     * @return Integer. Number of users.
     */
    public Integer findNumberUsers(Integer itemId, Integer userId){
		
   	 Object result = (Object) getSession()
                .createSQLQuery("select users from item_users where item_id=:itemId AND user_id=:userId")
                .setParameter("itemId", itemId)
                .setParameter("userId", userId)
                .uniqueResult();
   	 	//System.out.println(result+" number of users for item "+itemId+" and user "+userId);
   	 	if(result==null){
   	 		return new Integer(0);
   	 	}
   	 	else if(result instanceof BigDecimal){
   	 		return Integer.valueOf(((BigDecimal)result).intValue());
   	 	}
   	 	else{
   	 		return (Integer)result;
   	 	}
    	
    }
  
    public List<OrderDTO> findNormalByUser(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.ne("isMaster", 1))
                .add(Restrictions.ne("addToMaster", 1))
                .createAlias("baseUserByUserId", "u")
                    .add(Restrictions.eq("u.id", userId));
        
        return criteria.list();
    }
    
    /**
     * Returns the description of the items with the number of users for the given customer
     * @param userId
     * @return List<Object[]>. Object[0]=description, Object[1]=number of users
     */
	@SuppressWarnings("unchecked")
	public List<Object[]> findItemUsersWithDescription(Integer userId){
		
      	 Query query = getSession()
                   .createSQLQuery("select b.content,a.item_id,a.users from international_description b, item_users a " +
                   		"where b.table_id=14 AND b.foreign_id=a.item_id AND b.language_id=1 AND a.user_id=:userId AND a.users>0 ORDER BY a.item_id ASC")
                   .setParameter("userId", userId);
      	 	
       	return query.list();
       }
	
	/**
     * Returns a list of Integer, containing the itemIds of the products that the given customer has already bought.
     * @param userId
     * @return List<Integer>. itemIds
     */
	public List<Integer> getItemUsersItems(Integer userId){
		Query query = getSession()
                .createSQLQuery("select a.item_id from item_users a " +
                		"where a.user_id=:userId AND a.users>0 ORDER BY a.item_id ASC")
                .setParameter("userId", userId);
		
    	
    	if(query.list()!=null && !query.list().isEmpty() && query.list().get(0) instanceof BigDecimal){
    		 List<Integer> x=new ArrayList<Integer>();
    		 List<BigDecimal> a=query.list();
    		for(int i=0;i<a.size();i++){
        		x.add(Integer.valueOf(a.get(i).intValue()));
        	}
    		return x;
    	 }
    	 else if(query.list()!=null){
    		return (ArrayList<Integer>)query.list();
    	 }
    	 else{
    		 return new ArrayList<Integer>();
    	 }
	}
    
    
}
