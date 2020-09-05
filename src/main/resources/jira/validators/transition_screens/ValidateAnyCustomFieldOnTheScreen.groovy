package jira.validators.transition_screens

import webwork.action.ActionContext
import com.opensymphony.workflow.InvalidInputException

def request = ActionContext.request
if (request) {
    def cfValue = request.parameterMap.get("customfield_10007").toString()
    // [-1] is an expected null value.
    if (cfValue == "[-1]") throw new InvalidInputException ("Please specify Extension")
}