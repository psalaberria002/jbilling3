package com.sapienter.jbilling.server.pluggableTask;

import java.math.BigDecimal;
import java.util.Set;

import com.sapienter.jbilling.server.invoice.NewInvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;

public class SetQuantityToOneTask extends PluggableTask
implements InvoiceCompositionTask {

	@Override
	public void apply(NewInvoiceDTO invoice, Integer userId)
			throws TaskException {
		ItemDAS itemDas=new ItemDAS();
		//Find items in invoice that need to be invoiced with 1 as quantity
		for (int i = 0; i < invoice.getResultLines().size(); i++) {
            InvoiceLineDTO invoiceLine = (InvoiceLineDTO) invoice.getResultLines().get(i);
            ItemDTO item = invoiceLine.getItem();
            
            if (item != null) {
            	//Change the quantity and price to the products that have to be invoiced with 1 as quantity 
                if(itemDas.hasToBeQuantityOne(item.getId()).equals(1)){
                	invoiceLine.setQuantity(1); 	
                	invoiceLine.setPrice(invoiceLine.getAmount().divide(invoiceLine.getQuantity()));// price=amount/quantity
                }
            }
        }
		
	}

	@Override
    public BigDecimal calculatePeriodAmount(BigDecimal fullPrice, PeriodOfTime period) {
        throw new UnsupportedOperationException("Can't call this method");
    }
	
}