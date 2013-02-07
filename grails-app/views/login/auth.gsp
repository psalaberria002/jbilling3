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

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>

<head>
    <meta name="layout" content="public" />

    <title><g:message code="login.page.title"/></title>

    <script type='text/javascript'>
        $(document).ready(function() {
            $('#login input[name="j_username"]').focus();
        });
    </script>
</head>
<body>

    <g:render template="/layouts/includes/messages"/>

    <div id="login" class="form-edit">
        <div class="heading">
            <strong><g:message code="login.prompt.title"/></strong>
        </div>
        <div class="form-hold">
            <form action='${postUrl}' method='POST' id='login-form' autocomplete='on'>
                <fieldset>

                    <div class="form-columns">
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="login.prompt.username"/></content>
                            <content tag="label.for">username</content>
                            <g:textField class="field" name="j_username" value="${params.userName}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="login.prompt.password"/></content>
                            <content tag="label.for">password</content>
                            <g:passwordField class="field" name="j_password"/>
                        </g:applyLayout>
                        
                        <%-- Shows the list of companies when login 
                  
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="login.prompt.client.id"/></content>
                            <content tag="label.for">client_id</content>
                            <g:select name="j_client_id"
                                      from="${CompanyDTO.list()}"
                                      optionKey="id"
                                      optionValue="description"
                                      value="${params.companyId}"/>
                        </g:applyLayout>
                         --%>
                         <g:hiddenField name="j_client_id" value="10"/><%--Automatically chooses the company with the id in value--%>
                         
                         <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="login.prompt.remember.me"/></content>
                            <content tag="label.for">${rememberMeParameter}</content>
                            <g:checkBox class="cb checkbox" name="${rememberMeParameter}" checked="${hasCookie}"/>
                        </g:applyLayout>

                        <br/>
                    </div>

                    <div class="buttons">
                        <ul>
                            <li>
                                <a href="#" class="submit save" onclick="$('#login form').submit();">
                                    <span><g:message code="login.button.submit"/></span>
                                </a>
                            </li>
                        </ul>
                    </div>
                </fieldset>
            </form>
        </div>
    </div>

</body>
</html>