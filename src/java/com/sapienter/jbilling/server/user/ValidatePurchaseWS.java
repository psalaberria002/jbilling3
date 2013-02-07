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

package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Result object for validatePurchase API method.
 */
public class ValidatePurchaseWS implements Serializable {

    private Boolean success;
    private String[] message;
    private Boolean authorized;
    private String quantity;

    public ValidatePurchaseWS() {
        success = true;
        message = null;
        authorized = true;
        quantity = "0.0";
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String[] getMessage() {
        return message;
    }

    public void setMessage(String[] message) {
        this.message = message;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public void setAuthorized(Boolean authorized) {
        this.authorized = authorized;
    }

    public String getQuantity() {
        return quantity;
    }

    public BigDecimal getQuantityAsDecimal() {
        return quantity == null ? null : new BigDecimal(quantity);
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public void setQuantity(Double quantity) {
        this.setQuantity(new BigDecimal(quantity));
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = (quantity != null ? quantity.toString() : null);
    }
}
