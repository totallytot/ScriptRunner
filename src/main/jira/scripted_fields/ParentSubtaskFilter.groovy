def parentIssue = issue.getParentObject();
def parentIssueKey, issueKey;

if (parentIssue != null) {
	parentIssueKey = parentIssue.getKey();
    return parentIssueKey;
}
else {
    issueKey = issue.getKey();
    return issueKey;
}
