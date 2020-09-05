package jira.validators.transition_screens

import com.opensymphony.workflow.InvalidInputException
import webwork.action.ActionContext

def request = ActionContext.request
if (request) {
    def parameterMap = request.parameterMap
    log.warn "parameterMap " + parameterMap.toString()
    def groupPickerVal = parameterMap.get("customfield_10413").toString()
    log.warn "groupPickerVal " + groupPickerVal
    def assignee = parameterMap.get("assignee").toString()
    log.warn assignee
    // for null value
    if (groupPickerVal == "[]") {
        throw new InvalidInputException ('Пожалуйста, укажите значение в поле "Рабочая группа".')
    }
    else if (groupPickerVal == "[Service Desk]") {
        throw new InvalidInputException ('Рабочая группа не должна быть Service Desk.')
    }
}