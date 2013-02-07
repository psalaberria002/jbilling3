package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import junit.framework.TestCase;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.payment.db.*;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.Constants;


import java.lang.String;
import java.lang.System;
import java.math.BigDecimal;
import java.util.Calendar;


public class DynamicBalanceManagerTaskTest extends TestCase {

    private JbillingAPI api;
    private static int USER_ID;
    private static final int PAYMENT_METHOD_ID = 1;
    private static int  US_DOLLAR_ID = 1;
    private static int  AUS_DOLLAR_ID = 11;
    private static int PAYMENT_ID;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        api = JbillingAPIFactory.getAPI();
    }

    /**
     * Test Scenario: Make a user with currency AUS $ , then make a payment of US$ 100, the dynamic balance of the user should be greater than AUS $ 101 , assuming that US $ is STRONGER than AUS $
     */
    public void testDetermineAmountWithPaymentSuccessfulEvent() {

        try {
            System.out.println("Testing determine amount with payment successful event");
            String username = "user-payment-succ-15";
            String email = "user-payment-succ@jbilling.com";
            String balance = "1";
            // make a new user with ZERO balance and currency as AUS DOLLAR
            USER_ID = makeUser(AUS_DOLLAR_ID, username, email, balance);
            PaymentWS payment = new PaymentWS();
            payment.setUserId(USER_ID);
            //payment.setCheque(CHEQUE);
            payment.setMethodId(PAYMENT_METHOD_ID);
            Calendar calendar = Calendar.getInstance();
            payment.setPaymentDate(calendar.getTime());
            payment.setCurrencyId(US_DOLLAR_ID);
            payment.setAmount("100");
            payment.setIsRefund(0);
            // check that user's dynamic balance is 100
            UserWS user =  api.getUserWS(USER_ID);
            System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
            assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal())==0));
            api.createPayment(payment);
            // update the user
            api.updateUser(user);
            user = api.getUserWS(USER_ID);
            System.out.println("User's dynamic balance now is "+user.getDynamicBalanceAsDecimal());
            System.out.println("Payment amount "+payment.getAmountAsDecimal());
            System.out.println("balance "+new BigDecimal(balance));
            
            assertTrue("User's balance must be greater than the topped up value, assuming US $ is stronger than AUS $",(user.getDynamicBalanceAsDecimal().compareTo(payment.getAmountAsDecimal().add(new BigDecimal(balance)))>0));
        }catch(Exception e) {
            System.out.println("Exception "+e);
            e.printStackTrace();
        }
    }


    /**
     * Test Scenario: Make a payment in different currency than user's default currency , then delete the payment, the result should be that user's dynamic balance should be updated according to the user's currency
     *  and not on the payment's currency value.
     */
    public void testDetermineAmountWithPaymentDeletedEvent() {

        try {
            System.out.println("Testing determine amount with payment successful event");
            String username = "user-payment-del-2";
            String email = "user-payment-succ@jbilling.com";
            String balance = "1";
            // make a new user with ZERO balance and currency as AUS DOLLAR
            USER_ID = makeUser(AUS_DOLLAR_ID, username, email, balance);
            PaymentWS payment = new PaymentWS();
            payment.setUserId(USER_ID);
            //payment.setCheque(CHEQUE);
            payment.setMethodId(PAYMENT_METHOD_ID);
            Calendar calendar = Calendar.getInstance();
            payment.setPaymentDate(calendar.getTime());
            payment.setCurrencyId(US_DOLLAR_ID);
            payment.setAmount("100");
            payment.setIsRefund(0);
            // check that user's dynamic balance is 100
            UserWS user =  api.getUserWS(USER_ID);
            System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
            assertTrue("User's Balance would be ONE", (BigDecimal.ONE.compareTo(user.getDynamicBalanceAsDecimal())==0));
            PAYMENT_ID = api.createPayment(payment);
            // update the user
            api.updateUser(user);
            user = api.getUserWS(USER_ID);
            System.out.println("User's dynamic balance now is "+user.getDynamicBalanceAsDecimal());
            System.out.println("Payment amount "+payment.getAmountAsDecimal());
            System.out.println("balance "+new BigDecimal(balance));
            assertTrue("User's balance must be greater than the topped up value, assuming US $ is stronger than AUS $",(user.getDynamicBalanceAsDecimal().compareTo(payment.getAmountAsDecimal().add(new BigDecimal(balance)))>0));
            
            // now delete the payment , user's dynamic balance should become equal to initial value
            api.deletePayment(PAYMENT_ID);
            // update the user
            api.updateUser(user);
            user = api.getUserWS(USER_ID);
            
            System.out.println("User's dynamic balance is now "+user.getDynamicBalanceAsDecimal());
            assertTrue("User's Dynamic Balance Must Be Back to initial balance ", (user.getDynamicBalanceAsDecimal().compareTo(new BigDecimal(balance))==0));
          
        }catch(Exception e) {
            System.out.println("Exception "+e);
            e.printStackTrace();
        }
    }


    
    private int makeUser(int currencyId,String username,String email, String balance) {
        System.out.println("Making User");
        UserWS user = new UserWS();
        user.setCurrencyId(currencyId);
        user.setUserName(username);
        user.setPassword("123qwe");
        user.setContact(new ContactWS());
        user.getContact().setEmail(email);
        System.out.println("Creating user");
        user.setMainRoleId(5);// customer role
        user.setRole("Customer");
        user.setLanguageId(new Integer(1));
        user.setBalanceType(Constants.BALANCE_PRE_PAID);
        user.setDynamicBalance(new BigDecimal(balance));
        return api.createUser(user);
    }
}