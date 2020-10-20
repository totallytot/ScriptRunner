package jira.script_console

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleActors;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.security.roles.RoleActor
import com.atlassian.jira.security.roles.actor.GroupRoleActorFactory

import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.jira.mail.Email;
import com.atlassian.mail.MailException;

//script checks specified groups, removes them from project roles and sends the report

StringBuilder output = new StringBuilder();
ProjectManager projectManager = ComponentAccessor.getProjectManager();

String group = "jira-administrators";
String group1 = "jira-software-users";

List<String> groups = new ArrayList();
groups.add(group);
groups.add(group1);


ProjectRoleManager projectRoleManager = (ProjectRoleManager) ComponentAccessor.getComponentOfType(ProjectRoleManager.class);

final Collection<ProjectRole> projectRoles = projectRoleManager.getProjectRoles();

for (Project project : projectManager.getProjectObjects())

{
    for (ProjectRole projectRole : projectRoles)
    {
        final ProjectRoleActors projectRoleActors = projectRoleManager.getProjectRoleActors(projectRole, project);

        for (RoleActor actor : projectRoleActors.getRoleActors()) {

            if (actor.getDescriptor().equals(group) || actor.getDescriptor().equals(group1)) {
                output.append("Project Key: ")
                        .append(project.getKey())
                        .append(" | Project Role: ")
                        .append(projectRole.getName())
                        .append(" | Removed Group: ")
                        .append(actor.getDescriptor())
                        .append("\n")
                        .append(System.getProperty("line.separator"));
            }
        }
    }
}

for (String gr : groups) {

    projectRoleManager.removeAllRoleActorsByNameAndType(gr, GroupRoleActorFactory.GroupRoleActor.GROUP_ROLE_ACTOR_TYPE);

}

if (output.length() > 1) {
    SMTPMailServer smtpMailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer();
    if (smtpMailServer != null) {
        Email email = new Email("xxxxi@yyy.com");
        email.setSubject("JIRA Security Issue");
        email.setBody(output.toString());

        try {
            smtpMailServer.send(email);
            log.debug("JIRA Security Issuer: Mail sent");
        } catch (MailException e) {
            log.error(e.getMessage());
        }
    }
    else log.warn("Check SMTPMailServer configuration");
}
return output.toString();