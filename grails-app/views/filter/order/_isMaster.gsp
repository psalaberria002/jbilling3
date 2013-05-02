
<div id="${filter.name}">
    <span class="title <g:if test='${filter.value}'>active</g:if>"><g:message code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:select name="filters.${filter.name}.integerValue"
                              from="${[0, 1]}"
                              valueMessagePrefix='filters.isMaster'
                              noSelection="['': message(code: 'filters.isMaster.empty')]"/>
                </div>
                <label for="filters.${filter.name}.integerValue"><g:message code="filters.isMaster.label"/></label>
            </div>
        </fieldset>
    </div>
</div>