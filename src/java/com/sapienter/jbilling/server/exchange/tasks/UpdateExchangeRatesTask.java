package com.sapienter.jbilling.server.exchange.tasks;


import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDAS;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDTO;
import com.sapienter.jbilling.tools.UpdateExchangeRates;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class UpdateExchangeRatesTask extends AbstractCronTask {
    private static final Logger LOG = Logger.getLogger(UpdateExchangeRatesTask.class);

    

    public String getTaskName() {
        return "exchange rate update process: " + getScheduleString();
    }

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		UpdateExchangeRates uer=new UpdateExchangeRates();
		Iterator it = uer.getMap().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry e = (Map.Entry)it.next();
			System.out.println(e.getKey() + " " + e.getValue());
			CurrencyDAS cDas=new CurrencyDAS();
			Integer cid=cDas.findIdByCode((String)e.getKey());
			CurrencyExchangeDAS cedas=new CurrencyExchangeDAS();
			Integer ceid=cedas.findExchangeId(0,cid);
			System.out.println(ceid+" ceid");
			cedas.updateExchangeRateById(ceid, (BigDecimal)e.getValue());
		}
		
		
	}

    


}
