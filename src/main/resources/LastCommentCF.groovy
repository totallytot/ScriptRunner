//Script Location: Sript Field
//Shows last comment for each JIRA issue. Usefull in search for issues.
//String output was rendered to wiki.

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.RendererManager
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext

def commentManager = ComponentAccessor.getCommentManager()
def comment = commentManager.getLastComment(issue)
String lastComment = null
if (comment != null) {
    lastComment = comment.getBody()
    RendererManager rendererManager = ComponentAccessor.getRendererManager();
    JiraRendererPlugin wikiRenderer = rendererManager.getRendererForType("atlassian-wiki-renderer")
    IssueRenderContext renderContext = new IssueRenderContext(issue)
    lastComment = wikiRenderer.render(lastComment, renderContext)
}
return lastComment
