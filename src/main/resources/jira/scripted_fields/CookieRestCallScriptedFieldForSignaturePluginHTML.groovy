package jira.scripted_fields

import groovy.xml.MarkupBuilder
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON
import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def sessionRequest = new HTTPBuilder("https://jira.example.com/rest/auth/1/session")
def authResponse = sessionRequest.request(POST, JSON) { request ->
    body = [username: 'auto', password: 'secret']
    request.setHeader("Content-Type", "application/json")
}
def signatureRequest = new HTTPBuilder("https://jira.example.com/rest/electronic-signature/1.0/signature/${issue.key}")
def signatures = signatureRequest.request(GET, JSON) { request ->
    request.setHeader("Content-Type", "application/json")
    request.setHeader("Cookie", "JSESSIONID=${authResponse.get('session').get('value')}")
}
def sdf = new SimpleDateFormat("dd MMM yyyy HH:mm")
def writer = null
if (signatures) {
    writer = new StringWriter()
    def builder = new MarkupBuilder(writer)
    builder.table(class: "aui") {
        thead {
            tr {
                th("User")
                th("Date")
                th("From Status")
                th("To Status")
            }
        }
        tbody {
            signatures.each { signature ->
                tr {
                    td("${ComponentAccessor.userManager.getUserByKey(signature.get("userKey")).displayName}")
                    td("${sdf.format(signature.get("date"))}")
                    td("${signature.get("fromStatus")}")
                    td("${signature.get("toStatus")}")
                }
            }
        }
    }
    writer.toString()
}