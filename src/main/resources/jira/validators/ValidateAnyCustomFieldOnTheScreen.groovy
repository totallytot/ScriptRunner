package jira.validators

import webwork.action.ActionContext
import com.opensymphony.workflow.InvalidInputException

def request = ActionContext.getRequest()
if (request) {
    def cfValue = request.getParameterMap().get("customfield_10007").toString()
    if (cfValue == "[-1]") throw new InvalidInputException ("Please specify Extension")    //[-1] is an expected null value
}