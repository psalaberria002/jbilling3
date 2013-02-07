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

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestCase;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * WSTest
 *
 * @author Brian Cowdery
 * @since 28/03/11
 */
public class WSTest extends PricingTestCase {

    private static final Integer MONTHLY_PERIOD = 2;

    public void testPooledPricing() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        enablePricingPlugin(api);


        final Integer LONG_DISTANCE_PLAN_ITEM = 2700;       // used to determine the pool size
        final Integer LONG_DISTANCE_CALL = 2800;            // has the pooled pricing

        UserWS user = new UserWS();
        user.setUserName("pooled-pricing-test");
        user.setPassword("password");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(5);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        user.setBalanceType(Constants.BALANCE_NO_DYNAMIC);

        ContactWS contact = new ContactWS();
        contact.setEmail("test@test.com");
        contact.setFirstName("Pricing Test");
        contact.setLastName("Pooled Pricing");
        user.setContact(contact);

        user.setUserId(api.createUser(user)); // create user
        assertNotNull("customer created", user.getUserId());



        // create a pooled plan
        // for every unit of "LONG_DISTANCE_PLAN_ITEM", user gets 5 units of "LONG_DISTANCE_CALL" included.
        // overage is charged at $1.00/unit
        PriceModelWS pooledPrice = new PriceModelWS(PriceModelStrategy.POOLED.name(), new BigDecimal("1.00"), 1);
        pooledPrice.addAttribute("pool_item_id", LONG_DISTANCE_PLAN_ITEM.toString());
        pooledPrice.addAttribute("multiplier", "5");

        PlanItemWS callPrice = new PlanItemWS();
        callPrice.setItemId(LONG_DISTANCE_CALL);
        callPrice.setModel(pooledPrice);

        PlanWS plan = new PlanWS();
        plan.setItemId(LONG_DISTANCE_PLAN_ITEM);
        plan.setDescription("Pooled long distance calls.");
        plan.setPeriodId(MONTHLY_PERIOD);
        plan.addPlanItem(callPrice);

        plan.setId(api.createPlan(plan)); // create plan
        assertNotNull("plan created", plan.getId());



        // subscribe the customer to the pooled pricing plan
        // because we count usage of the pooled item, this order should be a main subscription
        // to define the period within which to look for usage of the LONG_DISTANCE_PLAN_ITEM
        OrderWS order = new OrderWS();
    	order.setUserId(user.getUserId());
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(MONTHLY_PERIOD);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setIsCurrent(1); // is main subscription

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(LONG_DISTANCE_PLAN_ITEM);
        line.setUseItem(true);
        line.setQuantity(1);
        order.setOrderLines(new OrderLineWS[] { line });

        order.setId(api.createOrder(order)); // create order
        order = api.getOrder(order.getId());
        assertNotNull("order created", order.getId());



        // rate an order to test the pool
        OrderWS testOrder = new OrderWS();
    	testOrder.setUserId(user.getUserId());
        testOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        testOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        testOrder.setCurrencyId(1);
        testOrder.setActiveSince(new Date());

        // test that a purchase of 5 units (equal to the included pool size)
        // rates at zero
        OrderLineWS testLine = new OrderLineWS();
        testLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        testLine.setItemId(LONG_DISTANCE_CALL);
        testLine.setUseItem(true);
        testLine.setQuantity(5); // equal to pool size
        testOrder.setOrderLines(new OrderLineWS[] { testLine });

        testOrder = api.rateOrder(testOrder);

        assertEquals(testOrder.getOrderLines().length, 1);
        assertEquals(new BigDecimal("5"), testOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals(new BigDecimal("0.00"), testOrder.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("0.00"), testOrder.getOrderLines()[0].getAmountAsDecimal());

        // test that a purchase over the 5 included units
        // rates at 1.00 per extra unit
        testLine = new OrderLineWS();
        testLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        testLine.setItemId(LONG_DISTANCE_CALL);
        testLine.setUseItem(true);
        testLine.setQuantity(7); // 2 units over pool size
        testOrder.setOrderLines(new OrderLineWS[] { testLine });

        testOrder = api.rateOrder(testOrder);

        assertEquals(testOrder.getOrderLines().length, 1);
        assertEquals(new BigDecimal("7"), testOrder.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals(new BigDecimal("0.29"), testOrder.getOrderLines()[0].getPriceAsDecimal());  // $0.29 * 7
        assertEquals(new BigDecimal("2.00"), testOrder.getOrderLines()[0].getAmountAsDecimal()); // = 1.999999



        // cleanup
        disablePricingPlugin(api);
        api.deletePlan(plan.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getUserId());
    }

}
