package jira.post_functions

def  issuekey=issue.getKey()
def issueReporter=issue.getReporter()
def issueReporterMail=issueReporter.getEmailAddress()


def baseurl = "http://jenkins/buildByToken/buildWithParameters?job=auto_actionitems/dbauto_actionitems_single_ticket&token=singletickettoken&environment=all_lower&ticket=${issuekey}&email=${issueReporterMail}&cause=jira_workflow_trigger"

def connection=new URL(baseurl).openConnection() as HttpURLConnection
    connection.setRequestMethod( "POST" )
               connection.doOutput = true
               connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
			   connection.getOutputStream().write()
               connection.connect()
def connection_response_code = connection.getResponseCode();
def connection_response_text=connection.getInputStream().getText()
log.debug  "Post Function Log ${connection_response_code} ${baseurl}"
