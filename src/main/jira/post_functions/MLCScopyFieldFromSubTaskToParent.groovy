package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

def subComponentCF = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {it.name == 'Sub-Component'}
def parentSubComponentCFValue = (ArrayList<LazyLoadedOption>) issue.getCustomFieldValue(subComponentCF)

if (parentSubComponentCFValue) {
    def lastChildOption = parentSubComponentCFValue.get(parentSubComponentCFValue.size() - 1)
    def optionsChain = []
    optionsChain << lastChildOption
    def parentOption = lastChildOption.parentOption
    while (parentOption) {
        optionsChain << parentOption
        parentOption = parentOption.parentOption
    }
    Collections.reverse(optionsChain)
    def subComponentCFfromParent = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue.parentObject).find {
        it.name == 'Sub-Component'
    }
    subComponentCFfromParent.updateValue(null, issue.parentObject, new ModifiedValue(null, optionsChain), new DefaultIssueChangeHolder())
}