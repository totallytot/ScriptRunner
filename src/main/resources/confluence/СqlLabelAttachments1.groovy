package confluence

import com.atlassian.confluence.api.model.pagination.PageResponse
import com.atlassian.confluence.api.model.pagination.SimplePageRequest
import com.atlassian.confluence.api.service.search.CQLSearchService
import com.atlassian.confluence.labels.Label
import com.atlassian.confluence.labels.LabelManager
import com.atlassian.confluence.pages.AttachmentManager
import com.atlassian.plugin.osgi.container.OsgiContainerManager
import com.atlassian.spring.container.ContainerManager
import com.atlassian.confluence.api.model.content.Content

final String LABEL_VALUE = "test_bulk_label"
final String CQL_QUERY = "type = attachment and label != ${LABEL_VALUE} and lastmodified > 2019-11-14"
final int MAX_ENTITIES = 500
def label = new Label(LABEL_VALUE)

def osgiContainerManager = ContainerManager.getComponent("osgiContainerManager") as OsgiContainerManager
def cqlSearchService = osgiContainerManager.getServiceTracker(CQLSearchService.class.name).service as CQLSearchService

def pageRequest = new SimplePageRequest(0, MAX_ENTITIES)
def searchResult = cqlSearchService.searchContent(CQL_QUERY, pageRequest) as PageResponse<Content>
searchResult.each {
    addLabel(label, it.id.asLong())
}
def nextBatchStartIndex = searchResult.results.size()
while (searchResult.hasMore()) {
    def nextPageRequest = new SimplePageRequest(nextBatchStartIndex, MAX_ENTITIES)
    searchResult = cqlSearchService.searchContent(CQL_QUERY, nextPageRequest)
    searchResult.each {
        addLabel(label, it.id.asLong())
    }
    nextBatchStartIndex += searchResult.results.size()
}
return "Done"

static def addLabel(Label label, Long id) {
    def labelManager = ContainerManager.getComponent("labelManager") as LabelManager
    def attachmentManager = ContainerManager.getComponent("attachmentManager") as AttachmentManager
    def attachment = attachmentManager.getAttachment(id)
    // if statement for confluence version < 7 - added by customer, not actual for us
    if (!attachment.getLabels().contains(label)) labelManager.addLabel(attachment, label)
}