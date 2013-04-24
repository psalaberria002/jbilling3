package com.sapienter.jbilling.server.exchange.tasks;


import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

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
import org.xml.sax.SAXException;


public class UpdateExchangeRatesTask extends AbstractCronTask {
    private static final Logger LOG = Logger.getLogger(UpdateExchangeRatesTask.class);

    

    public String getTaskName() {
        return "exchange rate update process: " + getScheduleString();
    }

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		UpdateExchangeRates uer=null;
		try {
			uer = new UpdateExchangeRates();
		} catch (ParserConfigurationException
				| SAXException | IOException e1) {
			
			e1.printStackTrace();
		}
		Iterator it = uer.getMap().entrySet().iterator();
		CurrencyDAS cDas=new CurrencyDAS();
		CurrencyExchangeDAS cedas=new CurrencyExchangeDAS();
		while (it.hasNext()) {
			Map.Entry e = (Map.Entry)it.next();
			LOG.debug(e.getKey() + " " + e.getValue());
			Integer cid=cDas.findIdByCode((String)e.getKey());
			if(cid!=null){
				Integer ceid=cedas.findExchangeId(0,cid);
				cedas.updateExchangeRateById(ceid, (BigDecimal)e.getValue());
			}
			
		}
		
		
	}

    


}
