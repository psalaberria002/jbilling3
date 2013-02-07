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

package com.sapienter.jbilling.server.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Locale;

import junit.framework.TestCase;

import com.sapienter.jbilling.server.entity.AchDTO;
import com.sapienter.jbilling.server.entity.CreditCardDTO;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

//


/**
 * @author Alexander Aksenov, Vikas Bodani
 * @since 30.04.11
 */
public class SimpleTaxCompositionTest extends TestCase {
    
    private JbillingAPI api;

    private final static String PLUGIN_PARAM_TAX_ITEM_ID= "tax_item_id";
    private final static String PLUGIN_PARAM_EXEMPT_ITEM_CATEGORY_ID= "item_exempt_category_id";

    private static final int ADMIN_USER_ID = 1;
    private final static int EXEMPT_ITEM_ID = 2700;
    private final static int USUAL_ITEM_ID = 2602;
    private static int TAX_ITEM_ID= -1;
    private static int TAXABLE_CUSTOMER_ID= -1;
    
    private static Integer item_exempt_category_id= Integer.valueOf(2200);

    private final static BigDecimal TAX_ITEM_COST = new BigDecimal(10.0);
    private final static BigDecimal TAX_ITEM_PERCENTAGE = new BigDecimal(10.0);
    private final static BigDecimal FIXED_ORDER_COST = new BigDecimal(25);
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        api = JbillingAPIFactory.getAPI();
    }
    
    public void testAddSimpleTaxPlugin() {
        try {
            System.out.println("testAddSimpleTaxPlugin");

            //create tax product
            ItemDTOEx taxItem= new ItemDTOEx();
            taxItem.setNumber("Tax101");
            taxItem.setGlCode("TAX101");
            taxItem.setPrice(TAX_ITEM_COST);
            taxItem.setHasDecimals(Integer.valueOf(1));
            taxItem.setEntityId(Integer.valueOf(1));
            taxItem.setDescription("Tax Item");
            taxItem.setCurrencyId(Integer.valueOf(1));
            taxItem.setTypes(new Integer[]{Integer.valueOf(22)});
            
            Integer taxItemID= api.createItem(taxItem);
            assertNotNull("Tax Item Created. ", taxItemID);
            TAX_ITEM_ID= taxItemID.intValue();
            
            //Create a new object from the form
            PluggableTaskWS newTask = new PluggableTaskWS();
            newTask.setTypeId(Integer.valueOf(86));//SimpleTaxCompositionTask
            newTask.setProcessingOrder(Integer.valueOf(3));
            newTask.setNotes("Simple tax task");
            newTask.setVersionNumber(1);
            newTask.getParameters().put(PLUGIN_PARAM_TAX_ITEM_ID, taxItemID.toString());
            newTask.getParameters().put(PLUGIN_PARAM_EXEMPT_ITEM_CATEGORY_ID, item_exempt_category_id.toString());
            Integer pluginId = api.createPlugin(newTask);
            
            assertNotNull("Plugin added successfully.", pluginId);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    public void testInvoiceWithoutExemptItem() {
        try {

            System.out.println("testInvoiceWithoutExemptItem");
            UserWS user1= generateUser("simpleTaxTest1", false, false);

            Integer taxableUserId= api.createUser(user1);
            
            assertNotNull("user created. ", taxableUserId);
            
            TAXABLE_CUSTOMER_ID= taxableUserId.intValue();
            
            OrderWS orderWS = createOrderWS(taxableUserId, USUAL_ITEM_ID, FIXED_ORDER_COST);
            // create order
            Integer orderId = api.createOrder(orderWS);
            OrderWS createdOrder = api.getOrder(orderId);
            assertNotNull("Order should be created", createdOrder);
            // invoice createdOrder
            Integer createdInvoiceId = api.createInvoiceFromOrder(orderId, null);
            // check for invoice created
            assertNotNull("Invoice should be created", createdInvoiceId);
            InvoiceWS invoice = api.getInvoiceWS(createdInvoiceId);
            boolean hasTaxLine = false;
            for (InvoiceLineDTO lineDto : invoice.getInvoiceLines()) {
                if (lineDto.getItemId().intValue() == TAX_ITEM_ID) {
                    hasTaxLine = true;
                    break;
                }
            }
            assertTrue("Invoice should contains tax line", hasTaxLine);
            assertEquals("Total by invoice should be 25 + 10", invoice.getTotalAsDecimal().compareTo(TAX_ITEM_COST.add(FIXED_ORDER_COST)), 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    public void testInvoiceWithExemptItem() {
        try {
            System.out.println("testInvoiceWithExemptItem");
            OrderWS orderWS = createOrderWS(TAXABLE_CUSTOMER_ID, EXEMPT_ITEM_ID, FIXED_ORDER_COST);
            OrderLineWS lines[] = new OrderLineWS[2];
            lines[0] = orderWS.getOrderLines()[0];
            OrderLineWS line = createOrderLine(USUAL_ITEM_ID, FIXED_ORDER_COST);
            lines[1] = line;
            orderWS.setOrderLines(lines);

            // create order
            Integer orderId = api.createOrder(orderWS);
            OrderWS createdOrder = api.getOrder(orderId);
            assertNotNull("Order should be created", createdOrder);
            // switch tax item to percentage
            ItemDTOEx taxItem = api.getItem(TAX_ITEM_ID, ADMIN_USER_ID, new PricingField[]{});
            taxItem.setPercentage(TAX_ITEM_PERCENTAGE);
            api.updateItem(taxItem);
            try {
                // invoice createdOrder
                Integer createdInvoiceId = api.createInvoiceFromOrder(orderId, null);
                // check for invoice created
                assertNotNull("Invoice should be created", createdInvoiceId);
                InvoiceWS invoice = api.getInvoiceWS(createdInvoiceId);
                boolean hasTaxLine = false;
                for (InvoiceLineDTO lineDto : invoice.getInvoiceLines()) {
                    if (lineDto.getItemId() == TAX_ITEM_ID) {
                        hasTaxLine = true;
                        break;
                    }
                }
                assertTrue("Invoice should contains tax line", hasTaxLine);
                assertEquals("Total by invoice should be 25 * (1 + 0.1) + 25", invoice.getTotalAsDecimal().setScale(2, RoundingMode.HALF_UP), FIXED_ORDER_COST.add(FIXED_ORDER_COST.multiply(new BigDecimal(1.1))).setScale(2, RoundingMode.HALF_UP));
            } finally {
                // restore item as flat cost
                String pct=null;
                taxItem.setPercentage(pct);
                api.updateItem(taxItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    public void testInvoiceWithOnlyExemptItem() {
        try {
            System.out.println("testInvoiceWithOnlyExemptItem");
            OrderWS orderWS = createOrderWS(TAXABLE_CUSTOMER_ID, EXEMPT_ITEM_ID, FIXED_ORDER_COST);
            // create order
            Integer orderId = api.createOrder(orderWS);
            OrderWS createdOrder = api.getOrder(orderId);
            assertNotNull("Order should be created", createdOrder);
            // invoice createdOrder
            Integer createdInvoiceId = api.createInvoiceFromOrder(orderId, null);
            // check for invoice created
            assertNotNull("Invoice should be created", createdInvoiceId);
            InvoiceWS invoice = api.getInvoiceWS(createdInvoiceId);
            boolean hasTaxLine = false;
            for (InvoiceLineDTO lineDto : invoice.getInvoiceLines()) {
                if (lineDto.getItemId() == TAX_ITEM_ID) {
                    hasTaxLine = true;
                    break;
                }
            }
            assertTrue("Invoice should CONTAINS tax line with FLAT price for EXEMPT item", hasTaxLine);
            //flat price for tax applies even for exempt item category
            assertEquals("Total by invoice should be 25 + 10", invoice.getTotalAsDecimal().compareTo(FIXED_ORDER_COST.add(TAX_ITEM_COST).setScale(2, RoundingMode.HALF_UP)), 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }
    
    
//    Note: We are not testing for Exempt customer as of now
//    public void testInvoiceForExemptCustomer() {
//        try {
//            System.out.println("");
//            OrderWS orderWS = createOrderWS(EXEMPT_CUSTOMER_ID, USUAL_ITEM_ID, FIXED_ORDER_COST);
//
//            // create order
//            Integer orderId = api.createOrder(orderWS);
//            OrderWS createdOrder = api.getOrder(orderId);
//            assertNotNull("Order should be created", createdOrder);
//            // invoice createdOrder
//            Integer createdInvoiceId = api.createInvoiceFromOrder(orderId, null);
//            // check for invoice created
//            assertNotNull("Invoice should be created", createdInvoiceId);
//            InvoiceWS invoice = api.getInvoiceWS(createdInvoiceId);
//            boolean hasTaxLine = false;
//            for (InvoiceLineDTO lineDto : invoice.getInvoiceLines()) {
//                if (lineDto.getItemId() == TAX_ITEM_ID) {
//                    hasTaxLine = true;
//                    break;
//                }
//            }
//            assertFalse("Invoice should NOT contains tax line", hasTaxLine);
//            assertEquals("Total by invoice should be 25", invoice.getTotalAsDecimal().compareTo(FIXED_ORDER_COST), 0);
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail("Exception caught:" + e);
//        }
//    }

    private OrderWS createOrderWS(int userId, int itemId, BigDecimal price) {
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(userId);
        newOrder.setBillingTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID);
        newOrder.setPeriod(2); // monthly
        newOrder.setCurrencyId(1);
        newOrder.setNotes("notes");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2011, 0, 1);
        newOrder.setCycleStarts(cal.getTime());
        newOrder.setActiveSince(cal.getTime());
        cal.clear();
        cal.set(2011, 1, 15);
        newOrder.setActiveUntil(cal.getTime());
        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line = createOrderLine(itemId, price);
        lines[0] = line;
        newOrder.setOrderLines(lines);
        return newOrder;
    }

    private OrderLineWS createOrderLine(int itemId, BigDecimal price) {
        OrderLineWS line = new OrderLineWS();
        line.setPrice(price);
        line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setAmount(price);
        line.setDescription("Item " + itemId + " Line.");
        line.setItemId(itemId);
        return line;
    }

    private static UserWS generateUser(String userName, boolean addCc, boolean addAch) {
        UserWS newUser = new UserWS();
        newUser.setUserName(userName);
        newUser.setPassword("abc123");
        newUser.setLanguageId(new Integer(1));
        newUser.setMainRoleId(new Integer(5));//customer
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(1);
        newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);

        // add a contact
        ContactWS contact = new ContactWS();
        contact.setEmail("test@test.com");
        contact.setFirstName("Test");
        contact.setLastName("User");
        newUser.setContact(contact);

        if (addCc) {
            // add a credit card
            CreditCardDTO cc = new CreditCardDTO();
            cc.setName("Frodo Baggins");
            cc.setNumber("4111111111111152");
            Calendar expiry = Calendar.getInstance();
            expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
            cc.setExpiry(expiry.getTime());

            newUser.setCreditCard(cc);
        }

        if (addAch) {
            AchDTO ach = new AchDTO();
            ach.setAbaRouting("123456789");
            ach.setAccountName("Test User");
            ach.setAccountType(Integer.valueOf(1));
            ach.setBankAccount("123456789");
            ach.setBankName("ABC Bank");

            newUser.setAch(ach);
        }
        return newUser;
    }

}
