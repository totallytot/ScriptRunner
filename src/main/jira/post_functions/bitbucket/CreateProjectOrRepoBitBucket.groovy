package jira.post_functions.bitbucket

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.user.DelegatingApplicationUser
import groovyx.net.http.HTTPBuilder
import org.apache.log4j.Level
import org.apache.log4j.Logger

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.*

def log = Logger.getLogger("check")
log.setLevel(Level.DEBUG)

def cfScope = ComponentAccessor.customFieldManager.getCustomFieldObjects()
def requestField = cfScope.find{it.name=="Request Type"}
def pNameField = cfScope.find{it.name=="Project name"}
def pKeyField = cfScope.find{it.name=="Project key"}
def pOwnersField = cfScope.find{it.name=="Consumer"}
def pNameBBField = cfScope.find{it.name=="BitBucket Project"}
def repoNameField = cfScope.find{it.name=="Repository name"}
def description = issue.getDescription()
String commentBody = ""
List <String> errorsCollection = new ArrayList()
if (((LazyLoadedOption)issue.getCustomFieldValue(requestField)).value=="Создание проекта"){
    log.debug("Create project")
	def projectKey = pKeyField.getValue(issue).toString().replaceAll("[^a-zA-Z0-9\\_]","").toUpperCase()
    def projectName = pNameField.getValue(issue).toString().replaceAll("[^a-zA-Z0-9\\.\\-\\_]","").toUpperCase()
    if (projectName.length()>64) projectName = projectName.substring(0,63)
    def json = ["key":"KEY", "name": "PROJECTNAME", "description": "test project"]
    json["key"]=projectKey
    json["name"]=projectName
    json["description"]=description
    def uri = "/rest/api/latest/projects"    
	def result = sendReq(uri, json, POST)
    if (result == 201) {
	    log.debug("Project Created ${baseURL}/projects/${projectKey}")   
        commentBody = "Project Created ${baseURL}/projects/${projectKey}"
        uri = "/rest/api/1.0/projects/${projectKey}/permissions/users?name=Admin"
	    result = sendReq(uri, null, DELETE)
        if (result == 204) log.debug("Admin deleted from project")
        else {
                log.debug("Admin not deleted from project")
                errorsCollection.add("Admin not deleted from project")
        }        
	    //Выдаем права группе itd_service_desk админские
	    uri = "/rest/api/1.0/projects/${projectKey}/permissions/groups?name=itd_service_desk&permission=PROJECT_ADMIN"
	    result = sendReq(uri, null, PUT)
        if (result == 204) log.debug("itd_service_desk added to project")
        else {
                log.debug("itd_service_desk not added to project")
                errorsCollection.add("itd_service_desk not added to project")
        }
	    //Выдаем права группе bitbucket-internal на запись
	    uri = "/rest/api/1.0/projects/${projectKey}/permissions/groups?name=bitbucket-internal&permission=PROJECT_WRITE"
	    result = sendReq(uri, null, PUT)
        if (result == 204) log.debug("bitbucket-internal added to project")
        else {
                log.debug("bitbucket-internal not added to project")
                errorsCollection.add("bitbucket-internal not added to project")
        }
	    //Выдаем права владельцам        
        pOwnersField.getValue(issue).each{DelegatingApplicationUser user ->
            uri = "/rest/api/1.0/projects/${projectKey}/permissions/users?name=${user.getKey()}&permission=PROJECT_WRITE"
            result = sendReq(uri, null, PUT)
            if (result == 204) log.debug("${user.getDisplayName()} added to project")
            else {
                log.debug("${user.getDisplayName()} not added to project")
                errorsCollection.add(user.getDisplayName()+" not added to project")
            }
        }
    }
    else {
        if (result == 400){
            log.debug("The project was not created due to a validation error.")
            errorsCollection.add("The project was not created due to a validation error.")
        }
        else{
            if (result == 409){
                log.debug("The project key or name is already in use.")
            	errorsCollection.add("The project key or name is already in use.")
            }
        }
    }
}
else {
    log.debug("Create repo")
    def repoName = repoNameField.getValue(issue).toString().replaceAll("[^a-zA-Z0-9\\ ]","").toLowerCase().trim()
    def repoKey = repoName.replaceAll(" ", "-")
    def projectKey = pNameBBField.getValue(issue).toString()
    def json = ["name": "reponame"]    
    json["name"]=repoName
    def uri = "/rest/api/latest/projects/${projectKey}/repos"    
	def result = sendReq(uri, json, POST)
    if (result == 201) {
	    log.debug("Repo Created ${baseURL}/projects/${projectKey}/repos/${repoKey}")       
        commentBody = "Repo Created ${baseURL}/projects/${projectKey}/repos/${repoKey}"     
        //Выдаем права владельцам        
        pOwnersField.getValue(issue).each{DelegatingApplicationUser user ->
            uri = "/rest/api/1.0/projects/${projectKey}/repos/${repoKey}/permissions/users?name=${user.getKey()}&permission=REPO_ADMIN"
            result = sendReq(uri, null, PUT)
            if (result == 204) log.debug("${user.getDisplayName()} added to repo")
            else {
                log.debug("${user.getDisplayName()} not added to repo")
                errorsCollection.add(user.getDisplayName()+" not added to repo")
            }
        }
    }
    else {
        if (result == 400){
            log.debug("The repository was not created due to a validation error.")
            errorsCollection.add("The repository was not created due to a validation error.")
        }
        else{
            if (result == 409){
                log.debug("A repository with same name already exists.")
            	errorsCollection.add("A repository with same name already exists.")
            }
        }
    }
}

    

def sendReq (def uri, def json, groovyx.net.http.Method method){
    def baseURL = "${baseURL}"
    def url = "${baseURL}${uri}"    
    def http = new HTTPBuilder(url)
	def request = http.request(method, JSON) { req ->
        body = json
   		req.setHeader("Content-Type", "application/json") 
    	req.setHeader("Authorization", "Basic !!!auth!!!" )
	    response.success = { resp, JSON ->
  			return resp.status            
    	}    
        response.failure = { resp ->
        	return resp.status           
        }
    }
}
    

