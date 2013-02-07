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

<div class="table-box">
    <table cellpadding="0" cellspacing="0">
        <thead>
            <th><g:message code="title.notification.category" />
            </th>
        </thead>
        <tbody>
            <g:each in="${lst}" status="idx" var="dto">
                <tr class="${dto.id == (selected)? 'active' : '' }">
                    <td><g:remoteLink breadcrumb="id" class="cell"
                            action="list" id="${dto.id}"
                            params="['template': 'list']"
                            before="register(this);"
                            onSuccess="render(data, next);"
                        >
                            <strong> ${dto.getDescription(session['language_id'])}
                            </strong>
                        </g:remoteLink></td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>
<div class="btn-box">
    <div class="row"></div>
</div>