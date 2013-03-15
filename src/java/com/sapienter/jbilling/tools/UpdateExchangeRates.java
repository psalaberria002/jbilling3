package com.sapienter.jbilling.tools;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class UpdateExchangeRates {
	private Map<String,BigDecimal> map;

	public UpdateExchangeRates(){
		map=new HashMap<String,BigDecimal>();
		map.put("NOK", new BigDecimal(5.4));
		map.put("USD", new BigDecimal(1));
		map.put("EUR", new BigDecimal(0.8));
		
	}
	public Map<String, BigDecimal> getMap(){
		return map;
	}
}
