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

import java.math.BigDecimal;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.invoice.NewInvoiceDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;

/**
 * This simple task gets the days to add to the invoice date from the 
 * billing process configuration. It doesn't get into any other consideration,
 * like business days, etc ...
 * @author Emil
 */
public class CalculateDueDate
    extends PluggableTask
    implements InvoiceCompositionTask {

    /* (non-Javadoc)
     * @see com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask#apply(com.sapienter.betty.server.invoice.NewInvoiceDTO)
     */
    public void apply(NewInvoiceDTO invoice, Integer userId) throws TaskException {
        // set up
        Date generated = invoice.getBillingDate();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(generated);
        Logger.getLogger(CalculateDueDate.class).debug(
                "Calculating due date from " + cal.getTime());
        
        // get the days configures
        try {
            // add the period of time
            cal.add(MapPeriodToCalendar.map(invoice.getDueDatePeriod().getUnitId()), 
                    invoice.getDueDatePeriod().getValue().intValue());
            // set the due date
            invoice.setDueDate(cal.getTime());
        } catch (Exception e) {
            Logger.getLogger(CalculateDueDate.class).error("Exception:", e);
            throw new TaskException(e);
        }
       
    }
    
    public BigDecimal calculatePeriodAmount(BigDecimal fullPrice, PeriodOfTime period) {
        throw new UnsupportedOperationException("Can't call this method");
    }


}
