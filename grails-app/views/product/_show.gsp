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

<%@ page import="com.sapienter.jbilling.server.util.Util"%>

<%--
  Product details template. This template shows a product and all the relevant product details.

  @author Brian Cowdery
  @since  16-Dec-2010
--%>


<div class="column-hold">
    <div class="heading">
	    <strong>${selectedProduct.internalNumber}</strong>
	</div>

	<div class="box">
        <!-- product info -->
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="product.detail.id"/></td>
                    <td class="value">${selectedProduct.id}</td>
                </tr>
                <tr>
                    <td><g:message code="product.detail.internal.number"/></td>
                    <td class="value">${selectedProduct.internalNumber}</td>
                </tr>
                <tr>
                    <td><g:message code="product.detail.gl.code"/></td>
                    <td class="value">${selectedProduct.glCode}</td>
                </tr>
                <tr>
                    <td><g:message code="product.detail.percentage"/></td>
                    <td class="value">
                        <g:if test="${selectedProduct.percentage}">
                            <g:formatNumber number="${selectedProduct.percentage}" formatName="percentage.format"/>
                        </g:if>
                        <g:else>
                            -
                        </g:else>
                    </td>
                </tr>

                <g:each var="price" in="${selectedProduct.itemPrices.sort{ it.currencyDTO.id }}">
                    <tr>
                        <td>${price.currencyDTO.code}:&nbsp;</td>
                        <td class="value">
                            <g:formatNumber number="${price.price}" type="currency" currencySymbol="${price.currencyDTO.symbol}"/>
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>

        <!-- percentage excluded categories -->
        <g:if test="${selectedProduct.percentage}">
        <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
            <tbody>
                <tr class="price">
                    <td><g:message code="product.excludedCategories"/></td>
                    <td class="value">
                        <g:each var="category" status="i" in="${selectedProduct.excludedTypes.sort{ it.description }}">
                            ${category.description}<g:if test="${i < selectedProduct.excludedTypes.size()-1}">, </g:if>
                        </g:each>
                    </td>
                </tr>
            </tbody>
        </table>
        </g:if>        
        
        <!-- flags -->
        <table class="dataTableProduct" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><em><g:message code="product.detail.decimal"/></em></td>
                    <td class="value"><em><g:formatBoolean boolean="${selectedProduct.hasDecimals > 0}"/></em></td>
                </tr>
                <tr>
                    <td><em><g:message code="product.detail.manual.pricing"/></em></td>
                    <td class="value"><em><g:formatBoolean boolean="${selectedProduct.priceManual > 0}"/></em></td>
                </tr>
            </tbody>
        </table>

        <p class="description">
            ${selectedProduct.description}
        </p>
        <!-- product categories cloud -->
        <div class="box-cards box-cards-open">
            <div class="box-cards-title">
                <span><g:message code="product.detail.categories.title"/></span>
            </div>
            <div class="box-card-hold">
                <div class="content">
                    <ul class="cloud">
                        <g:each var="category" in="${selectedProduct.itemTypes.sort{ it.description }}">
                            <li>
                                <g:link action="list" id="${category.id}">${category.description}</g:link>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <div class="btn-box">
        <sec:ifAllGranted roles="PRODUCT_41">
            <g:link action="editProduct" id="${selectedProduct.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
        </sec:ifAllGranted>

        <sec:ifAllGranted roles="PRODUCT_42">
            <a onclick="showConfirm('deleteProduct-${selectedProduct.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
        </sec:ifAllGranted>
    </div>

    <g:render template="/confirm"
              model="['message': 'product.delete.confirm',
                      'controller': 'product',
                      'action': 'deleteProduct',
                      'id': selectedProduct.id,
                      'formParams': ['category': selectedCategoryId],
                      'ajax': true,
                      'update': 'column1',
                      'onYes': 'closePanel($(\'column2\'))'
                     ]"/>
</div>

