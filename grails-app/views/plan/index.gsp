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

<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO" %>
<html>
<head>
    <meta name="layout" content="panels" />
</head>
<body>

<content tag="column1">
    <div class="table-box">
        <div class="table-scroll">
            <table id="plans" cellspacing="0" cellpadding="0">
                <thead>
                    <tr>
                        <th><g:message code="plan.th.name"/></th>
                        <th class="medium"><g:message code="plan.th.item.number"/></th>
                        <th class="small"><g:message code="plan.th.products"/></th>
                    </tr>
                </thead>

                <tbody>
                    <tr id="plan-1" class="active">
                        <td>
                            <strong>Simple discount plan</strong>
                            <em><g:message code="table.id.format" args="[1]"/></em>
                        </td>
                        <td>
                            <strong>PLAN-A</strong>
                        </td>
                        <td>
                            <span>2</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="btn-box">
        <a href="#" class="submit add"><span><g:message code="button.create"/></span></a>
    </div>
</content>

<content tag="column2">
    <div class="column-hold">
        <div class="heading">
            <strong>PLAN-A</strong>
        </div>

        <!-- plan details -->
        <div class="box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="plan.id"/></td>
                        <td class="value">1</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.internal.number"/></td>
                        <td class="value">PLAN-A</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.description"/></td>
                        <td class="value">Simple discount plan</td>
                    </tr>
                    <tr>
                        <td><g:message code="order.label.period"/></td>
                        <td class="value">Monthly</td>
                    </tr>
                    <tr>
                        <td>USD</td>
                        <td class="value">
                            $20.00
                        </td>
                    </tr>
                </tbody>
            </table>

            <p class="description">
                Example pricing plan that discounts multiple items when subscribed to.
            </p>
        </div>

        <!-- plan prices -->
        <div class="heading">
            <strong><g:message code="builder.products.title"/></strong>
        </div>
        <div class="box">
            <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
                <tbody>
                    <!-- example long distance call pricing -->
                    <tr>
                        <td><g:message code="product.internal.number"/></td>
                        <td class="value" colspan="3">
                            CALL-LD
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="product.description"/></td>
                        <td class="value" colspan="3">
                            Long distance call
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.precedence"/></td>
                        <td class="value">-1</td>
                    </tr>
                    <tr>
                        <td colspan="4">&nbsp;</td>
                    </tr>
                    <tr class="price">
                        <td><g:message code="plan.model.type"/></td>
                        <td class="value"><g:message code="price.strategy.GRADUATED"/></td>
                        <td><g:message code="plan.model.rate"/></td>
                        <td class="value">$0.50</td>
                    </tr>
                    <tr class="attribute">
                        <td></td><td></td>
                        <td><g:message code="included"/></td>
                        <td class="value">500</td>
                    </tr>

                    <tr><td colspan="4"><hr/></td></tr>

                    <!-- example service fee -->
                    <tr>
                        <td><g:message code="product.internal.number"/></td>
                        <td class="value" colspan="3">SERVICE-FEE</td>
                    </tr>
                    <tr>
                        <td><g:message code="product.description"/></td>
                        <td class="value" colspan="3">Service Fee</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.precedence"/></td>
                        <td class="value">-1</td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.item.bundled.quantity"/></td>
                        <td class="value">1</td>

                        <td><g:message code="plan.bundle.label.add.if.exists"/></td>
                        <td class="value"><g:message code="plan.bundle.add.if.exists.false"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="plan.bundle.period"/></td>
                        <td class="value">Monthly</td>

                        <td><g:message code="plan.bundle.target.customer"/></td>
                        <td class="value"><g:message code="bundle.target.customer.SELF"/></td>
                    </tr>
                    <tr><td colspan="4">&nbsp;</td></tr>
                    <tr class="price">
                        <td><g:message code="plan.model.type"/></td>
                        <td class="value"><g:message code="price.strategy.METERED"/></td>

                        <td><g:message code="plan.model.rate"/></td>
                        <td class="value">$10.00</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="btn-box">
            <a href="#" class="submit edit"><span><g:message code="button.edit"/></span></a>
            <a href="#" class="submit delete"><span><g:message code="button.delete"/></span></a>
        </div>
    </div>
</content>

</body>
</html>