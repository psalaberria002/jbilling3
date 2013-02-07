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

<%@ page contentType="text/html;charset=UTF-8" %>

<div class="form-edit" style="width:450px">

    <div class="heading">
        <strong><g:message code="configuration.title.contact.fields"/></strong>
    </div>

    <div class="form-hold">
        <g:form name="save-customcontactfields-form" action="save">
            <g:hiddenField name="recCnt" value="${types?.size()}"/>
            <fieldset>
                <div class="form-columns" style="width:450px">
                    <div class="one_column" style="padding-right: 0px;">
                    <table class="innerTable" id="custom_fields" style="width: 100%;">
                        <thead class="innerHeader">
                             <tr>
                                <th><g:message code="contact.field.name"/></th>
                                <th class="medium"><g:message code="contact.field.datatype"/></th>
                                <th class="medium"><g:message code="contact.field.isReadOnly"/></th>
                             </tr>
                         </thead>
                         <tbody>
                            <g:each status="iter" var="type" in="${types}">
                                <tr>
                                    <td>
                                        <g:textField class="field" style="float: right;width: 150px" name="obj[${iter}].description" 
                                            value="${type.getDescriptionDTO(session['language_id'])?.content}"/>
                                    </td>
                                    <td class="medium">
                                        <g:select style="float: right; position: relative; width:90px"  class="field" name="obj[${iter}].dataType" from="['String','Integer', 'Decimal', 'Boolean']"
                                            value="${type?.dataType}" />
                                    </td>
                                    <td class="medium">
                                        <g:select style="float: center; width: 50px;position: relative;" class="field" 
                                            name="obj[${iter}].readOnly" keys="[1,0]" from="['Yes', 'No']" value="${type.readOnly}"/>
                                        <g:hiddenField name="obj[${iter}].companyId" value="${session['company_id']}"/>
                                        <g:hiddenField name="obj[${iter}].id" value="${type.id}"/>
                                    </td>
                                </tr>
                            </g:each>
                            <tr>
                                <td><g:textField class="field" style="float: right;width: 150px" name="description" value=""/>
                                </td>
                                <td class="medium">
                                    <g:select style="float: right; position: relative; width:90px" class="field" 
                                        name="dataType" from="['','String','Integer', 'Decimal', 'Boolean']" value="" />
                                </td>
                                <td class="medium">
                                    <g:select style="float: center; width: 50px;position: relative;" class="field" 
                                        name="readOnly" keys="[1,0]" from="['Yes', 'No']" value=""/>
                                    <g:hiddenField name="companyId" value="${session['company_id']}"/>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    </div>
                    <div class="row">&nbsp;<br></div>
                </div>
            </fieldset>
             <div class="btn-box">
                <a onclick="$('#save-customcontactfields-form').submit();" class="submit save"><span><g:message code="button.save"/></span></a>
                <g:link controller="config" action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
            </div>
        </g:form>
    </div>
</div>