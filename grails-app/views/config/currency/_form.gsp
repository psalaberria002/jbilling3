%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.util.Constants" %>

<div class="form-edit">
    <div class="heading">
        <strong><g:message code="currency.config.title"/></strong>
    </div>
    <div class="form-hold">
        <g:form name="save-currencies-form" url="[action: 'saveCurrencies']">
            <fieldset>
                <div class="form-columns single">
                    <g:applyLayout name="form/select">
                        <content tag="label"><g:message code="currency.config.label.default"/></content>
                        <content tag="label.for">defaultCurrencyId</content>
                        <g:select name="defaultCurrencyId" from="${currencies}"
                                optionKey="id"
                                optionValue="${{ it.getDescription(session['language_id']) }}"
                                value="${entityCurrency}"/>
                    </g:applyLayout>
                </div>


                <div class="form-columns single">
                    <table cellpadding="0" cellspacing="0" class="innerTable" width="100%">
                        <thead class="innerHeader">
                            <tr>
                                <th class=""></th>
                                <th class="left tiny2"><g:message code="currency.config.th.symbol"/></th>
                                <th class="left tiny2"><g:message code="currency.config.th.active"/></th>
                                <th class="left medium"><g:message code="currency.config.th.rate"/></th>
                                <th class="left medium"><g:message code="currency.config.th.sysRate"/></th>
                            </tr>
                        </thead>
                        <tbody>

                        <g:each var="currency" in="${currencies.sort{ it.id }}">
                            <tr>
                                <td class="innerContent">
                                    ${currency.getDescription(session['language_id'])}
                                    <g:hiddenField name="currencies.${currency.id}.id" value="${currency.id}"/>
                                </td>
                                <td class="innerContent">
                                    ${currency.symbol}
                                    <g:hiddenField name="currencies.${currency.id}.symbol" value="${currency.symbol}"/>
                                    <g:hiddenField name="currencies.${currency.id}.code" value="${currency.code}"/>
                                    <g:hiddenField name="currencies.${currency.id}.countryCode" value="${currency.countryCode}"/>
                                </td>
                                <td class="innerContent">
                                    <g:checkBox class="cb checkbox" name="currencies.${currency.id}.inUse" checked="${currency.inUse}"/>
                                </td>
                                <td class="innerContent">
                                    <div class="inp-bg inp4">
                                        <g:textField name="currencies.${currency.id}.rate" class="field" value="${formatNumber(number: currency.rate, formatName: 'exchange.format')}"/>
                                    </div>
                                </td>
                                <td class="innerContent" style="text-align: left;">

                                    <g:if test="${currency.id != 1}">
                                        %{-- editable rate --}%
                                        <div class="inp-bg inp4">
                                            <g:textField name="currencies.${currency.id}.sysRate" class="field" value="${formatNumber(number: currency.sysRate, formatName: 'exchange.format')}"/>
                                        </div>
                                    </g:if>
                                    <g:else>
                                        %{-- USD always has a rate of 1.00 --}%
                                        <strong>
                                            <g:formatNumber number="${currency.sysRate}" type="currency" currencySymbol="${currency.symbol}"/>
                                            <g:hiddenField name="currencies.${currency.id}.sysRate" value="${currency.sysRate}"/>
                                        </strong>
                                    </g:else>

                                </td>
                            </tr>
                        </g:each>

                        </tbody>
                    </table>
                 </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>


            </fieldset>
        </g:form>
    </div>

    <div class="btn-box">
        <div class="row">
            <g:remoteLink controller="config" action="editCurrency" class="submit add" before="register(this);" onSuccess="render(data, next);">
                <span><g:message code="button.create"/></span>
            </g:remoteLink>
        </div>
        <div class="row">
            <a onclick="$('#save-currencies-form').submit();" class="submit save"><span><g:message code="button.save"/></span></a>
            <g:link controller="config" action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
        </div>
    </div>
</div>