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

package com.sapienter.jbilling.server.util.db;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.order.db.OrderDTO;

/**
 * CountryDas
 *
 * @author Brian Cowdery
 * @since 15/02/11
 */
public class CountryDAS extends AbstractDAS<CountryDTO> {
	
	public CountryDTO getByCode(String code){
		Criteria criteria = getSession().createCriteria(CountryDTO.class)
                .add(Restrictions.eq("code", code));

        return findFirst(criteria);
	}

}
