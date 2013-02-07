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

<%@ page import="com.sapienter.jbilling.server.report.ReportExportFormat"%>

<%--
  Report details template.

  @author Brian Cowdery
  @since  07-Mar-2011
--%>

<div class="column-hold">
    <div class="heading">
        <strong><g:message code="${selected.name}"/></strong>
    </div>

    <g:form name="run-report-form" url="[action: 'run', id: selected.id]" target="_blank" method="GET">
        <div class="box">
            <!-- report info -->
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="report.label.id"/></td>
                    <td class="value">${selected.id}</td>
                </tr>
                <tr>
                    <td><g:message code="report.label.type"/></td>
                    <td class="value">${selected.type.getDescription(session['language_id'])}</td>
                </tr>
                <tr>
                    <td><g:message code="report.label.design"/></td>
                    <td class="value">
                        <em title="${selected.reportFilePath}">${selected.fileName}</em>
                    </td>
                </tr>
                </tbody>
            </table>

            <!-- report description -->
            <p class="description">
                ${selected.getDescription(session['language_id'])}
            </p>

            <hr/>

            <!-- report parameters -->
            <g:render template="/report/${selected.type.name}/${selected.name}"/>

            <br/>&nbsp;
        </div>

        <div class="btn-box">
            <a class="submit edit" onclick="$('#run-report-form').submit();">
                <span><g:message code="button.run.report"/></span>
            </a>

            <span>
                <g:select name="format"
                          from="${ReportExportFormat.values()}"
                          noSelection="['': message(code: 'report.format.HTML')]"
                          valueMessagePrefix="report.format"/>
            </span>
        </div>

    </g:form>
</div>

