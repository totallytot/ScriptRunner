package jira.validators

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField

//cfs instruction 1-20 of select list type
def slIds = [12341L, 12349L, 12351L, 12352L, 12353L, 12354L, 12355L, 12356L, 12357L, 12358L, 12359L, 12342L, 12360L,
             12343L, 12344L, 12345L, 12346L, 12348L, 12350L, 12347L]
def slCfs = []
slIds.each { id ->
    slCfs.add(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(id))
}
def selectListValidation = !slCfs.any { cf -> ((String)issue.getCustomFieldValue((CustomField) cf)) in ["Open", "Block delivery"]}

//cfs instruction 1-20 of textarea type
def textIds = [12300L, 12309L, 12310L, 12311L, 12312L, 12340, 12314L, 12315L, 12316L, 12317L, 12318L, 12301L, 12319L,
               12302L, 12303L, 12304L, 12305L, 12306L, 12307L, 12308L]
def textCfs = []
textIds.each { id ->
    textCfs.add(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(id))
}
def textValidation = !textCfs.any{cf -> ((String)issue.getCustomFieldValue((CustomField) cf)) == null}

return (textValidation && selectListValidation)