package confluence.scheduled_jobs

import com.atlassian.mail.Email
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.mail.ConfluenceMailServerManager
import groovy.xml.MarkupBuilder
import com.onresolve.scriptrunner.db.DatabaseUtil

//input data
def valueForSearch = "password"
def mailSubject = "Confluence Security Report: wiki. Search for: ${valueForSearch}"
def recipient = "user@example.com"
def cc = "user1@example.com"
def sqlStatement = """
select CONCAT('https://wiki.example.com/pages/viewpage.action?pageId=', bc.CONTENTID) as "Link", 
c.TITLE, s.SPACEKEY, s.SPACENAME, c.CONTENT_STATUS, c.VERSION, um.username as "Creator", c.CREATIONDATE, 
umm.username as "Modifier", c.LASTMODDATE from bodycontent bc
left join content c on c.CONTENTID = bc.CONTENTID
left join spaces s on c.SPACEID = s.SPACEID
left join user_mapping um on um.user_key = c.CREATOR
left join user_mapping umm on umm.user_key = c.LASTMODIFIER  
where s.SPACEKEY not in ('SED2', 'SEUD2', 'SELD', 'PUBSELD', 'IWI', 'PB') 
and c.CONTENTTYPE = 'PAGE' and lower(bc.BODY) like
 """ + "'%${valueForSearch}%' order by s.SPACEKEY;"

//mail body in html
def writer = new StringWriter()
def builder = new MarkupBuilder(writer)
def counter = 0

builder.html {
    head {
        style("""
            table { 
                font-family: arial, sans-serif;
                border-collapse: collapse;
                width: 100%;
            }
            th { 
                border: 1px solid black;             
                text-align: center;                     
                padding: 3px;
            }
            td { 
                border: 1px solid black;             
                text-align: left;                     
                padding: 1px;
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
                th("Link")
                th("Title")
                th("Space Key")
                th("Space Name")
                th("Content Status")
                th("Version")
                th("Creator")
                th("Created")
                th("Modifier")
                th("Updated")
            }
            DatabaseUtil.withSql('local_confl_db') { sql ->
                sql.rows(sqlStatement).each { row ->
                    tr {
                        td(++counter)
                        td("${row.link}")
                        td("${row.title}")
                        td("${row.spacekey}")
                        td("${row.spacename}")
                        td("${row.content_status}")
                        td("${row.version}")
                        td("${row.creator}")
                        td("${row.creationdate}")
                        td("${row.modifier}")
                        td("${row.lastmoddate}")
                    }
                }
            }
        }
    }
}
def body = writer.toString()
def confluenceMailServerManager = ComponentLocator.getComponent(ConfluenceMailServerManager) as ConfluenceMailServerManager
def mailServer = confluenceMailServerManager.getDefaultSMTPMailServer()
if (counter > 0 && mailServer) {
    Email email = new Email(recipient)
    email.setMimeType("text/html")
    email.setSubject(mailSubject)
    if (cc) email.setCc(cc)
    email.setBody(body)
    mailServer.send(email)
}