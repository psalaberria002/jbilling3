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

/*
 * Created on Oct 7, 2004
 *
 */
package com.sapienter.jbilling.server.order;

import java.util.Calendar;

/**
 * @author Emil
 *
 */
public class TimePeriod {
    private Integer unitId = null;
    private Integer value = null;
    private Boolean df_fm = null;
    private Long own_invoice = null;
    
    public boolean equals(Object another) {
        boolean retValue = false;
        
        if (another != null) {
            TimePeriod other = (TimePeriod) another;
            if (unitId.equals(other.getUnitId()) &&
                    value.equals(other.getValue())) {
                if (df_fm == null && other.getDf_fm() == null) {
                    retValue = true;
                } else if (df_fm != null && other.getDf_fm() != null &&
                        df_fm.booleanValue() == 
                            other.getDf_fm().booleanValue()){
                    retValue = true;
                }
            }
            
            if (retValue) {
                retValue = own_invoice.equals(other.getOwn_invoice());
            }
        }
        return retValue;
    }
    
    /*
     * No need to add the own invoice here. You can return the same hash code
     * for two unequal objects.
     */
    public int hashCode() {
        int dfValue;
        if (df_fm == null) {
            dfValue = 0;
        } else if (df_fm.booleanValue()) {
            dfValue = 1;
        } else {
            dfValue = 2;
        }
        return unitId.intValue() * 100 + value.intValue() * 10 + dfValue;
    }
    
    public String toString() {
        return "Period unit " + unitId + " value " + value + " Df Fm " + df_fm;
    }
    public Integer getUnitId() {
        return unitId;
    }
    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }
    public Integer getValue() {
        return value;
    }
    public void setValue(Integer value) {
        this.value = value;
    }
    public Boolean getDf_fm() {
        return df_fm;
    }
    public void setDf_fm(Boolean df_fm) {
        this.df_fm = df_fm;
    }
    public void setDf_fm(Integer df_fm) {
        if (df_fm == null) {
            this.df_fm = null;
        } else {
            this.df_fm = new Boolean(df_fm.intValue() == 1);
        }
    }

    public Long getOwn_invoice() {
        return own_invoice;
    }
    public void setOwn_invoice(Integer own_invoice) {
        if (own_invoice != null && own_invoice.intValue() == 1) {
            // give a unique number to it
            Calendar cal = Calendar.getInstance();
            this.own_invoice = new Long(cal.getTimeInMillis());
        } else {
            this.own_invoice = new Long(0);
        }
    }
}
