package jira.REST_endpoints

import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

/*
Select List Conversions
https://scriptrunner.adaptavist.com/5.6.1/jira/behaviours-conversions.html
 */

@BaseScript CustomEndpointDelegate delegate

bitbucketProjects(httpMethod: "GET") { MultivaluedMap queryParams -> 

    def query = queryParams.getFirst("query") as String
	def baseURL = ""
    def rt = [:]
    if (query) {
        def url = "${baseURL}/rest/api/1.0/projects?name=${query}&limit=1000"
		def authRequest = new HTTPBuilder(url)
		def projects = authRequest.request(Method.GET, ContentType.JSON) { req ->
   				req.setHeader("Content-Type", "application/json") 
    			req.setHeader("Authorization", "Basic user:pass IN base64" )
		}
        if(projects){
            List <String> projectNames = projects["values"]*."name"
			rt = [
            	items: projectNames.collect { String project ->
                	[
                    	value: project,
                    	html : project.replaceAll(/(?i)${query}/) { "<b>${it}</b>" }, 
                    	label: project,
                	]
            	},
            	total: projects["total_count"],
           		footer: "Choose repo... (${projectNames.size()} of ${projects["total_count"]} shown...)"
        	]
    	return Response.ok(new JsonBuilder(rt).toString()).build()
        }
        else return Response.ok(new JsonBuilder(rt).toString()).build()    
	}
}

/* Behaviour Initialiser
getFieldByName("BitBucket Project").convertToSingleSelect([ 
    ajaxOptions: [
        url : getBaseUrl() + "/rest/scriptrunner/latest/custom/bitbucketProjects", 
        query: true, // keep going back to the sever for each keystroke
        minQueryLength: 2, 
        keyInputPeriod: 500, 
        formatResponse: "general", 
    ]
]) */