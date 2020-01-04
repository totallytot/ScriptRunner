package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor

def AttachmentM = ComponentAccessor.getAttachmentManager()
def Attachmelts = AttachmentM.getAttachments(issue)
def AttachmentSize = Attachmelts.size()
if (Attachmelts != null && AttachmentSize > 0){
    def AttachmentLast = Attachmelts[0].getCreated()

    for (def i=1;i<AttachmentSize;i++)
    {
        if (Attachmelts[i].getCreated()>AttachmentLast)
            AttachmentLast = Attachmelts[i].getCreated()
    }
    return AttachmentLast
}
