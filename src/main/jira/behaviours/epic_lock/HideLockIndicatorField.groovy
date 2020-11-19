package jira.behaviours.epic_lock

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def lockedForChangeField = getFieldByName("LockedForChange")
if (getFieldScreen().name == "PgM") lockedForChangeField.setHidden(false)
else lockedForChangeField.setHidden(true)