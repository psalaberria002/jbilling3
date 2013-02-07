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
package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

public abstract class PaymentTaskWithTimeout extends PaymentTaskBase {
    public static final String PARAM_TIMEOUT_SECONDS = "timeout_sec";
    
    private int myTimeout;
    
    @Override
    public void initializeParamters(PluggableTaskDTO task)
            throws PluggableTaskException {
        
        super.initializeParamters(task);

        String timeoutText = getOptionalParameter(PARAM_TIMEOUT_SECONDS, "10");
        try {
            myTimeout = Integer.parseInt(timeoutText);  
        } catch (NumberFormatException e){
            throw new PluggableTaskException("" // 
                    + "Integer expected for parameter: " + PARAM_TIMEOUT_SECONDS // 
                    + ", actual: " + timeoutText);
        }
    }
    
    protected int getTimeoutSeconds() {
        return myTimeout;
    }
}
