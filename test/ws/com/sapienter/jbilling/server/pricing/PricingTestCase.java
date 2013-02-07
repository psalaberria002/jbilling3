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

package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PricingTestCase
 *
 * @author Brian Cowdery
 * @since 28/03/11
 */
public class PricingTestCase extends TestCase {

    // plug-in configuration
    private static final Integer PRICING_PLUGIN_ID = 410;
    private static final Integer RULES_PRICING_PLUGIN_TYPE_ID = 61; // RulesPricingTask2
    private static final Integer MODEL_PRICING_PLUGIN_TYPE_ID = 79; // PriceModelPricingTask


    public PricingTestCase() {
    }

    public PricingTestCase(String name) {
        super(name);
    }


    /*
        Enable/disable the PricingModelPricingTask plug-in.
     */

    public void enablePricingPlugin(JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(PRICING_PLUGIN_ID);
        plugin.setTypeId(MODEL_PRICING_PLUGIN_TYPE_ID);

        api.updatePlugin(plugin);
    }

    public void disablePricingPlugin(JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(PRICING_PLUGIN_ID);
        plugin.setTypeId(RULES_PRICING_PLUGIN_TYPE_ID);

        api.updatePlugin(plugin);
    }


    /*
        Convenience assertions for BigDecimal comparisons.
     */

    public static void assertEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(null, expected, actual);
    }

    public static void assertEquals(String message, BigDecimal expected, BigDecimal actual) {
        assertEquals(message,
                     (Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
                     (Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP)));
    }

}
