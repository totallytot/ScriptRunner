package jira.scripted_fields

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON
import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def f = new SimpleDateFormat("dd MMM yyyy HH:mm")
def authRequest = new HTTPBuilder("https://jira.example.com/rest/auth/1/session")
def authResponse = authRequest.request(POST,JSON) { req ->
    body = [username: 'auto', password: 'secret']
    req.setHeader("Content-Type", "application/json")
}
def signaturesRequest = new HTTPBuilder("https://jira.example.com/rest/electronic-signature/1.0/signature/${issue.key}")
def signatures = signaturesRequest.request(GET,JSON) { req ->
    req.setHeader("Content-Type", "application/json")
    req.setHeader("Cookie", "JSESSIONID="+authResponse.get("session").get("value").toString())
}
if (signatures) {
// wiki markup style was used in JMCF (not scriptrunner scripted fields)
    String output = "||User||Date||From Status||To Status||\n"
    signatures.each {
        output += "|" + ComponentAccessor.userManager.getUserByKey(it.get("userKey").toString()).getDisplayName() + "|"
        output += f.format(new Date((it.get("date")))).toString() + "|"
        output += it.get("fromStatus").toString() + "|"
        output += it.get("toStatus").toString() + "|\n"
    }
    output
}
