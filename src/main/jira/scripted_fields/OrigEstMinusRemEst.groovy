package jira.scripted_fields
// Template: Duration (time-tracking)
def origEst = issue.originalEstimate
def remEst = issue.estimate
if (!origEst) return
return (origEst - remEst) //3600