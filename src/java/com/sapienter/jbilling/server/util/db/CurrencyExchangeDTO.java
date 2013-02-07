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


import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;


@Entity
@TableGenerator(
        name="currency_exchange_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="currency_exchange",
        allocationSize = 100
        )
@Table(name="currency_exchange")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CurrencyExchangeDTO  implements java.io.Serializable {


     private int id;
     private CurrencyDTO currencyDTO;
     private Integer entityId;
     private BigDecimal rate;
     private Date createDatetime;
     private int versionNum;

    public CurrencyExchangeDTO() {
    }

    
    public CurrencyExchangeDTO(int id, BigDecimal rate, Date createDatetime) {
        this.id = id;
        this.rate = rate;
        this.createDatetime = createDatetime;
    }
    public CurrencyExchangeDTO(int id, CurrencyDTO currencyDTO, Integer entityId, BigDecimal rate, Date createDatetime) {
       this.id = id;
       this.currencyDTO = currencyDTO;
       this.entityId = entityId;
       this.rate = rate;
       this.createDatetime = createDatetime;
    }
   
    @Id  @GeneratedValue(strategy=GenerationType.TABLE, generator="currency_exchange_GEN") 
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="currency_id")
    public CurrencyDTO getCurrency() {
        return this.currencyDTO;
    }
    
    public void setCurrency(CurrencyDTO currencyDTO) {
        this.currencyDTO = currencyDTO;
    }
    
    @Column(name="entity_id")
    public Integer getEntityId() {
        return this.entityId;
    }
    
    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }
    
    @Column(name="rate", nullable=false, precision=17, scale=17)
    public BigDecimal getRate() {
        return this.rate;
    }
    
    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
    
    @Column(name="create_datetime", nullable=false, length=29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }
    
    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }



}


