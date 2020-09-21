package jira.REST_endpoints.reports_with_gui

import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import com.atlassian.jira.component.ComponentAccessor

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@BaseScript CustomEndpointDelegate delegate

reports(httpMethod: "GET", groups: ["jira-administrators", "reports"]) {
    def url = "/rest/scriptrunner/latest/custom/reportGen"
    def usersAsOptions = getUsersAsOptions("jira-servicedesk-users")
    def dialog =
            """       
<section role="dialog" id="sr-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
<script>
    AJS.\$(".select2").auiSelect2();
    new AJS.DatePicker(AJS.\$("#source-range"), {'overrideBrowserDefault': true});
    new AJS.DatePicker(AJS.\$("#target-range"), {'overrideBrowserDefault': true});
    var dataObject = {};
    dataObject.srartDate = null;
    dataObject.endDate = null;
    dataObject.reportType = AJS.\$("#select2-report").val();
    dataObject.recipient = AJS.\$("#select2-recipient").val();
    AJS.\$("#source-range").change(function () {
        dataObject.srartDate = AJS.\$("#source-range").val();        
    });
    AJS.\$("#target-range").change(function () {
        dataObject.endDate = AJS.\$("#target-range").val();        
    });
    AJS.\$("#select2-report").change(function () {
        dataObject.reportType = AJS.\$("#select2-report").val();       
    });
    AJS.\$("#select2-recipient").change(function () {
        dataObject.recipient = AJS.\$("#select2-recipient").val();       
    });       
    AJS.\$("#send-button").on("click", function (e) {
        e.preventDefault();
        console.log(dataObject);
        if (dataObject.srartDate == null || dataObject.endDate == null) {
            AJS.flag({
                type: "error",
                body: "Пожалуйста, заполните даты!",
                close: "auto"
            });
        } else {
            AJS.\$.ajax({
                url: AJS.params.baseURL + "${url}",
                type: "POST",
                dataType: "json",
                contentType: "application/json",
                data: JSON.stringify(dataObject),
                success: function (resp) {
                    console.log(JSON.stringify(dataObject));
                    AJS.flag({
                        type: "success",
                        body: "Запрос на генерецаю отчета отправлен!",
                        close: "auto"
                    });
                },
                error: function (err) {
                    if (err.status == 502) {
                        AJS.flag({
                            type: "error",
                            body: "В данный период обращений нет!",
                            close: "auto"
                        });                   
                    } else {
                        AJS.flag({
                            type: "error",
                            body: "Произошла ошибка! Обратитесь к Администратору!",
                            close: "auto"
                        });
                    }
                },
                complete: function () {
                    AJS.dialog2("#sr-dialog").hide();
                    AJS.dialog2("#sr-dialog").remove();
                }
            });
        }
    });
</script> 
    <header class="aui-dialog2-header">
        <h2 class="aui-dialog2-header-main">Отчёт</h2>
    </header>
    
    <div class="aui-dialog2-content">            
        <div class="date-pickers-group">
            <aui-label for="source-range">Начало:</aui-label>
            <input class="aui-date-picker" id="source-range" type="date" max="2030-01-25" min="2020-01-01" />
            <aui-label for="target-range">Окончание:</aui-label>
            <input class="aui-date-picker" id="target-range" type="date" max="2030-01-25" min="2020-01-01" />
        </div>         
        <br>
        <div>
        	<aui-label for="select2-report">Тип отчёта:</aui-label>
            <select class="select2" id="select2-report" single="">
                <option value="tech-support">Оказание услуг технической поддержки</option>
                <option value="damage-control">Ремонтно-восстановительные работы</option>
            </select>
        </div>
        <br>
        <div>
            <aui-label for="select2-recipient">Получатель:</aui-label>
            <select class="select2" id="select2-recipient" single="">
                ${usersAsOptions}
            </select>
        </div>
    </div>
    
    <footer class="aui-dialog2-footer">
        <div class="aui-dialog2-footer-actions">
            <button id="send-button" class="aui-button aui-button-primary">Выслать на почту</button>
            <button id="dialog-close-button" class="aui-button aui-button-link">Закрыть</button>
        </div>
    </footer>          
</section>
"""
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()
}

static String getUsersAsOptions(String group) {
    def users = ComponentAccessor.groupManager.getUsersInGroup(group)
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def builder = new StringBuilder()
    users.each { user ->
        if (user != currentUser) builder.append("""<option value="${user.username}">${user.displayName}</option>""")
        else builder.append("""<option selected="selected" value="${user.username}">${user.displayName}</option>""")
    }
    builder.toString()
}