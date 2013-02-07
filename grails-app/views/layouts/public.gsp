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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8" />

        <title><g:layoutTitle default="jBilling" /></title>

        <link rel="shortcut icon" href="${resource(dir:'images', file:'favicon.ico')}" type="image/x-icon" />

        <link media="all" rel="stylesheet" href="${resource(dir:'css', file:'all.css')}" type="text/css" />
        <!--[if lt IE 8]><link rel="stylesheet" href="${resource(dir:'css', file:'lt7.css')}" type="text/css" media="screen"/><![endif]-->

        <g:javascript library="jquery" plugin="jquery"/>
        <g:javascript library="slideBlock" />
        <g:javascript library="clearinput" />
        <g:javascript library="main" />
        <g:javascript library="form" />
        <g:javascript library="checkbox" />

        <g:layoutHead/>
    </head>
    <body>
        <div id="wrapper">
            <!-- header -->
            <div id="header">
                <h1><a href="${resource(dir:'')}"></a></h1>
            </div>
            <div id="navigation">
                <ul></ul>
            </div>

            <!-- content -->
            <div id="main">
                <g:layoutBody />
            </div>
        </div>
    </body>
</html>