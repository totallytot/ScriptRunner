package jira.post_functions.watchers

import com.atlassian.jira.component.ComponentAccessor

def actualWatchers = []
if (issue.projectObject.key == "SD") {
    def watchers = [:]
    watchers["Резервное копирование"] = ["VProhorov"]
    watchers["JiraServiceDesk"] = ["azvonkov"]
    watchers["StaaS"] = ["VProhorov"]
    watchers["VMware"] = ["VProhorov"]
    watchers["Openstack"] = ["VProhorov"]
    watchers["ТехноСерв Диск"] = ["VProhorov"]
    watchers["Инфраструктура"] = ["VProhorov"]
    watchers["ОХ(хранилище)"] = ["VProhorov"]
    def serviceValue = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == "Сервис клиента" }?.getValue(issue) as String
    if (!serviceValue) return
    actualWatchers = watchers.find { serviceValue.contains(it.key as String) }.value as List
} else if (issue.projectObject.key == "IS") {
    actualWatchers = ["VProhorov", "MRaspertov"]
}
if (!actualWatchers || actualWatchers.isEmpty()) return
def userManger = ComponentAccessor.userManager
def watcherManager = ComponentAccessor.watcherManager
actualWatchers.each { username ->
    watcherManager.startWatching(userManger.getUserByName(username as String), issue)
}