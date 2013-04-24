package com.sapienter.jbilling.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.drools.lang.dsl.DSLMapParser.mapping_file_return;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sapienter.jbilling.server.process.task.NorwegianTaxCompositionTask;

public class UpdateExchangeRates {
	private Map<String, BigDecimal> map;

	public UpdateExchangeRates() throws ParserConfigurationException,
			MalformedURLException, SAXException, IOException {
		map = new HashMap<String, BigDecimal>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new URL(
				"http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml")
				.openStream());
		updateMap(doc, map);

	}

	public Map<String, BigDecimal> getMap() {
		return map;
	}

	public static void updateMap(Document doc, Map<String, BigDecimal> map) {
		NodeList nodes = doc.getElementsByTagName("Cube");
		BigDecimal eurRate = new BigDecimal(0);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				// a child element to process
				Element child = (Element) node;
				if (child.hasAttribute("currency")) {
					String attribute = child.getAttribute("currency");
					String attribute2 = child.getAttribute("rate");
					if (attribute.equals("USD")) {
						eurRate = (new BigDecimal(1)).divide(new BigDecimal(
								attribute2),4, BigDecimal.ROUND_HALF_UP);
						map.put("EUR", eurRate);
					} else if (attribute != null) {
						BigDecimal value=(new BigDecimal(attribute2)).multiply(eurRate).setScale(4, BigDecimal.ROUND_HALF_UP);
						map.put(attribute, value);
					}
				}

			}
		}

	}
}
