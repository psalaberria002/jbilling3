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

<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO; com.sapienter.jbilling.server.util.db.LanguageDTO; com.sapienter.jbilling.server.item.db.ItemTypeDTO" %>

<html>
<head>
    <meta name="layout" content="main" />

    <script type="text/javascript">
        $(document).ready(function() {
            $('#product\\.percentageAsDecimal').blur(function() {
                if ($(this).val()) {
                    $('#pricing :input').val('').attr('disabled', 'true');
                    $('#product\\.excludedTypes').attr('disabled', '');
                    closeSlide('#pricing');
                } else {
                    $('#pricing :input').attr('disabled', '');
                    $('#product\\.excludedTypes').val('').attr('disabled', 'true');
                    openSlide('#pricing');
                }
            }).blur();
        });
    </script>
</head>
<body>
<div class="form-edit">

    <g:set var="isNew" value="${!product || !product?.id || product?.id == 0}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="product.add.title"/>
            </g:if>
            <g:else>
                <g:message code="product.edit.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="save-product-form" action="saveProduct">
            <g:hiddenField name="selectedCategoryId" value="${categoryId}"/>
            <fieldset>
                <!-- product info -->
                <div class="form-columns">
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="product.id"/></content>

                            <g:if test="${isNew}"><em><g:message code="prompt.id.new"/></em></g:if>
                            <g:else>${product?.id}</g:else>

                            <g:hiddenField name="product.id" value="${product?.id}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="product.description"/></content>
                            <content tag="label.for">product.description</content>
                            <g:textField class="field" name="product.description" value="${product?.description}" size="40"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="product.percentage"/></content>
                            <content tag="label.for">product.percentageAsDecimal</content>
                            <g:textField class="field" name="product.percentageAsDecimal" value="${formatNumber(number: product?.percentage, maxFractionDigits: '4')}" size="6"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="product.allow.decimal.quantity"/></content>
                            <content tag="label.for">product.hasDecimals</content>
                            <g:checkBox class="cb checkbox" name="product.hasDecimals" checked="${product?.hasDecimals > 0}"/>
                        </g:applyLayout>
                    </div>

                    <div class="column">
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="product.internal.number"/></content>
                            <content tag="label.for">product.number</content>
                            <g:textField class="field" name="product.number" value="${product?.number}" size="40"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="product.gl.code"/></content>
                            <content tag="label.for">product.glCode</content>
                            <g:textField class="field" name="product.glCode" value="${product?.glCode}" size="40"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="product.categories"/></content>
                            <content tag="label.for">product.types</content>

                            <g:set var="types" value="${product?.types?.collect{ it as Integer }}"/>
                            <g:select name="product.types" multiple="true"
                                      from="${categories}"
                                      optionKey="id"
                                      optionValue="description"
                                      value="${types ?: categoryId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="product.excludedCategories"/></content>
                            <content tag="label.for">product.excludedTypes</content>

                            <g:set var="types" value="${product?.excludedTypes?.collect{ it as Integer }}"/>
                            <g:select name="product.excludedTypes" multiple="true"
                                      from="${categories}"
                                      optionKey="id"
                                      optionValue="description"
                                      value="${types}"/>
                        </g:applyLayout>
                    </div>
                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>

                <!-- pricing controls -->
                <div id="pricing" class="box-cards box-cards-open">
                    <div class="box-cards-title">
                        <a class="btn-open" href="#"><span><g:message code="product.prices"/></span></a>
                    </div>
                    <div class="box-card-hold">
                        <div class="form-columns">
                            <div class="column">
                                <g:each var="currency" in="${currencies.sort{ it.id }}">
                                    <g:applyLayout name="form/input">
                                        <content tag="label">${currency.getDescription(session['language_id'])}</content>
                                        <content tag="label.for">prices.${currency.id}</content>

                                        <g:set var="itemPrice" value="${product?.prices?.find { it.currencyId == currency.id }}"/>
                                        <g:textField class="field" name="prices.${currency.id}" value="${itemPrice?.price}"/>
                                    </g:applyLayout>
                                </g:each>
                            </div>

                            <div class="column">
                                <g:applyLayout name="form/checkbox">
                                    <content tag="label"><g:message code="product.allow.manual.pricing"/></content>
                                    <content tag="label.for">product.priceManual</content>
                                    <g:checkBox class="cb checkbox" name="product.priceManual" checked="${product?.priceManual > 0}"/>
                                </g:applyLayout>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>

                <div class="buttons">
                    <ul>
                        <li><a onclick="$('#save-product-form').submit();" class="submit save"><span><g:message code="button.save"/></span></a></li>
                        <li><g:link controller="product" action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link></li>
                    </ul>
                </div>

            </fieldset>
        </g:form>
    </div>

</div>
</body>
</html>
