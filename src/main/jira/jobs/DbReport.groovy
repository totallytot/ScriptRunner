package jira.jobs

import com.atlassian.jira.component.ComponentAccessor
import groovy.sql.Sql
import org.ofbiz.core.entity.ConnectionFactory
import org.ofbiz.core.entity.DelegatorInterface
import groovy.xml.MarkupBuilder
import com.atlassian.mail.Email

//input data
def valueForSearch = "password"
def mailSubject = "Jira Security Report. Search for ${valueForSearch} in comments."
def recipient = "0@example.biz"
def cc = "1@example.biz, 2@example.com"
def sqlStatement = """
 select p.pname, concat(p.pkey, '-', ji.issuenum) as "issuekey", 
 concat('https://jira.example.com/browse/', p.pkey, '-', ji.issuenum) as "link",
 ja.author, ja.created, ja.updateauthor, ja.updated, p.lead from jiraaction ja
 left join jiraissue ji on ji.id = ja.issueid
 left join project p on p.id = ji.project
 where lower(ja.actionbody) like 
 """ + "'%${valueForSearch}%';"

//connect to DB
def delegator = (DelegatorInterface) ComponentAccessor.getComponent(DelegatorInterface)
def helperName = delegator.getGroupHelperName("default")
def connection = ConnectionFactory.getConnection(helperName)
def sql = new Sql(connection)

//build html mail body
def writer = new StringWriter()
def builder = new MarkupBuilder(writer)
def counter = 1
try {
    builder.html {
        head {
            style("""
    table { 
        font-family: arial, sans-serif;
        border-collapse: collapse;
        width: 100%;
    }
     th, td { 
        border: 1px solid black;             
        text-align: left;                     
        padding: 3px;
    }
     tr:nth-child(even) {                   
        background-color: #dddddd;  
    }
    """)
        }
        body {
            table {
                tr {
                    th("Counter")
                    th("Project")
                    th("Issue Key")
                    th("Link")
                    th("Creator")
                    th("Created")
                    th("Update Author")
                    th("Updated")
                    th("Project Lead")
                }
                sql.eachRow(sqlStatement) { row ->
                    tr {
                        td(counter++)
                        td("${row.pname}")
                        td("${row.issuekey}")
                        td("${row.link}")
                        td("${row.author}")
                        td("${row.created}")
                        td("${row.updateauthor}")
                        td("${row.updated}")
                        td("${row.lead}")
                    }
                }
            }
        }
    }
} finally {
    sql.close()
}
def body = writer.toString()

//send mail
def mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()
if (mailServer) {
    Email email = new Email(recipient)
    email.setMimeType("text/html")
    email.setSubject(mailSubject)
    if (cc) email.setCc(cc)
    email.setBody(body)
    mailServer.send(email)
}
//for output in script console
//body