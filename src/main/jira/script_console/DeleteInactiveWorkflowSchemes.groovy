import com.atlassian.jira.component.ComponentAccessor

def schemeManager = ComponentAccessor.workflowSchemeManager

def sb = new StringBuffer()

schemeManager.schemeObjects.each {
 try{
   if(schemeManager.getProjectsUsing(schemeManager.getWorkflowSchemeObj(it.id)).size() == 0) {
     sb.append("Deleting workflow scheme: ${it.name}\n")
     schemeManager.deleteScheme(it.id)
   }
 }
 catch(Exception e) {
   //noop
   sb.append("Error: " + e + "\n");
 }
}

return sb.toString()
