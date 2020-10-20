package confluence.scheduled_jobs

import com.atlassian.mail.Email
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.mail.ConfluenceMailServerManager
import groovy.sql.Sql
import groovy.xml.MarkupBuilder
import java.sql.Driver

/**
 * Place mysql driver in confluence install/lib folder.
 */

//input data
def valueForSearch = "password"
def mailSubject = "Confluence Security Report. Search for: ${valueForSearch}"
def recipient = "0@example.biz"
def cc = "1@example.biz, 2@example.com"
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

//db connection
def driver = Class.forName("com.mysql.jdbc.Driver").newInstance() as Driver
def properties = new Properties()
properties.setProperty("user", "conf")
properties.setProperty("password", "secret")
def connection = driver.connect("jdbc:mysql://localhost:3306/confluence", properties)
def sql = new Sql(connection)

//mail body in html
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
                sql.eachRow(sqlStatement) { row ->
                    tr {
                        td(counter++)
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
} finally {
    sql.close()
    connection.close()
}
def body = writer.toString()
def confluenceMailServerManager = ComponentLocator.getComponent(ConfluenceMailServerManager) as ConfluenceMailServerManager
def mailServer = confluenceMailServerManager.getDefaultSMTPMailServer()
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