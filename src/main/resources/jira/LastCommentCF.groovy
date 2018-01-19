package jira
//Script Location: Sript Field
//Shows last comment for each JIRA issue. Usefull in search for issues.
//String output was rendered to wiki.

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext

def commentManager = ComponentAccessor.getCommentManager()
def comment = commentManager.getLastComment(issue)
String lastComment = null
if (comment != null) {
    lastComment = comment.getBody()
    def rendererManager = ComponentAccessor.getRendererManager()
    def wikiRenderer = rendererManager.getRendererForType("atlassian-wiki-renderer")
    def renderContext = new IssueRenderContext(issue)
    lastComment = wikiRenderer.render(lastComment, renderContext)
}
return lastComment
