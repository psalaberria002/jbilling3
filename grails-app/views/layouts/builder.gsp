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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <g:render template="/layouts/includes/head" model="[ajaxListeners: false]"/>
    <g:layoutHead/>
</head>
<body>
<div id="wrapper">
    <g:render template="/layouts/includes/header"/>

    <div id="main">
        <g:render template="/layouts/includes/breadcrumbs"/>

        <!-- content columns -->
        <div class="columns-holder">
            <div class="column panel" id="column1">
                <div class="column-hold">
                    <g:pageProperty name="page.builder"/>
                </div>
            </div>

            <div class="column panel">
                <div class="column-hold" id="column2">
                    <g:pageProperty name="page.review"/>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
