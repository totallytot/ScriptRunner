package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.option.Option

/*
Plugin: Multi-Level Cascading Select
Vendor: Sourcesense
https://marketplace.atlassian.com/apps/5008/multi-level-cascading-select?hosting=server&tab=overview
Was used as "Additional issue actions" in Create a sub-task SR post-function
Source article: https://addons.sourcesense.com/display/MCS/Set+the+value+of+an+MLCS+custom+field+using+ScriptRunner
 */
def subComponentCFfromParent = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue.parentObject).find {it.name == 'Sub-Component'}
def parentSubComponentCFValue = (List) issue.parentObject.getCustomFieldValue(subComponentCFfromParent)
if (parentSubComponentCFValue) {
//requires during parent creation only in order to remove from list com.sourcesense.jira.plugin.customfield.option.SpecialOptionFactory$SpecialOption@0 for 'None' from Multi-Level Cascading Select field
    def lazyValues = parentSubComponentCFValue.findAll {
        it instanceof com.atlassian.jira.issue.customfields.option.LazyLoadedOption
    }
    def lastChildOption = lazyValues.get(lazyValues.size() - 1) as Option

    def optionsChain = []
// We store the target option in the array optionsChain, then we add all of its ancestors
    optionsChain << lastChildOption
    def parentOption = lastChildOption.parentOption
    while (parentOption) {
        optionsChain << parentOption
        parentOption = parentOption.parentOption
    }
// We need to reverse the order of the options in the array so that the root-level one is the first
    Collections.reverse(optionsChain)
    def SubComponentCFfromChild = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
        it.name == 'Sub-Component'
    }

//in case sub-task is created via SR post-function
    issue.setCustomFieldValue(SubComponentCFfromChild, optionsChain)

//for regular post-functions
//subComponentCFfromParent.updateValue(null, issue.parentObject, new ModifiedValue(null, optionsChain), new DefaultIssueChangeHolder())
}