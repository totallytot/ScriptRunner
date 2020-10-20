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

final String CQL_QUERY = 'type = attachment and lastModified > "2020/05/15"'
final String LABEL_VALUE = "content_attachments"
final int MAX_ENTITIES = 500

def osgiContainerManager = ContainerManager.getComponent("osgiContainerManager") as OsgiContainerManager
def cqlSearchService = osgiContainerManager.getServiceTracker(CQLSearchService.class.name).service as CQLSearchService

def numberOfResultsFound = cqlSearchService.countContent(CQL_QUERY)
def finalResult = new ArrayList<Content>(numberOfResultsFound)
def pageRequest = new SimplePageRequest(0, MAX_ENTITIES)
def searchResult = cqlSearchService.searchContent(CQL_QUERY, pageRequest) as PageResponse<Content>
finalResult.addAll(searchResult.results)

while (searchResult.hasMore()) {
    def nextPageRequest = new SimplePageRequest(finalResult.size(), MAX_ENTITIES)
    searchResult = cqlSearchService.searchContent(CQL_QUERY, nextPageRequest)
    finalResult.addAll(searchResult.results)
}

def labelManager = ContainerManager.getComponent("labelManager") as LabelManager
def attachmentManager = ContainerManager.getComponent("attachmentManager") as AttachmentManager
def label = new Label(LABEL_VALUE)

finalResult.each {
    labelManager.addLabel(attachmentManager.getAttachment(it.id.asLong()), label)
}
return "Done"