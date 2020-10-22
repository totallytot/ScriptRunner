package jira.behaviours

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def group0 = "Группа архитектуры и эксплуатации сети"
def allowedValues0 = ["Добавление / изменение DNS записи", "Монтаж оборудования",
                      "Выделение Ethernet портов", "Резервирование Ethernet портов"]
def group1 = "Группа архитектуры и эксплуатации VMware"
def allowedValues1 = ["Добавление / изменение DNS записи", "Монтаж оборудования",
                      "Резервирование Ethernet портов VMware"]
def group2 = "Группа архитектуры и эксплуатации Cloudian"
def allowedValues2 = ["Добавление / изменение DNS записи", "Монтаж оборудования",
                      "Резервирование Ethernet портов Cloudian"]
def group3 = "Группа архитектуры и эксплуатации CEPH"
def allowedValues3 = ["Добавление / изменение DNS записи", "Монтаж оборудования",
                      "Резервирование Ethernet портов CEPH"]
def group4 = "Группа архитектуры и эксплуатации OpenStack"
def allowedValues4 = ["Добавление / изменение DNS записи", "Монтаж оборудования",
                      "Группа архитектуры и эксплуатации OpenStack"]
def group5 = "Группа эксплуатации СРК"
def allowedValues5 = ["Добавление / изменение DNS записи", "Монтаж оборудования"]

def customFieldManager = ComponentAccessor.customFieldManager
def optionsManager = ComponentAccessor.optionsManager
def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def userUtil = ComponentAccessor.userUtil

def allowedValues = []
if (userUtil.getGroupNamesForUser(currentUser.name).any { it == group0 })
    allowedValues = allowedValues0
else if (userUtil.getGroupNamesForUser(currentUser.name).any { it == group1 })
    allowedValues = allowedValues1
else if (userUtil.getGroupNamesForUser(currentUser.name).any { it == group2 })
    allowedValues = allowedValues2
else if (userUtil.getGroupNamesForUser(currentUser.name).any { it == group3 })
    allowedValues = allowedValues3
else if (userUtil.getGroupNamesForUser(currentUser.name).any { it == group4 })
    allowedValues = allowedValues4
else if (userUtil.getGroupNamesForUser(currentUser.name).any { it == group5 })
    allowedValues = allowedValues5
else return

// limit select list options
def customField = customFieldManager.getCustomFieldObject(fieldChanged)
def fieldConfig = customField.getRelevantConfig(issueContext)
def options = optionsManager.getOptions(fieldConfig)
def optionsMap = ["-1": "None"]
optionsMap += options.findAll { it.value in allowedValues }
        .collectEntries { [(it.optionId.toString()): it.value] }
def selectList = getFieldById(fieldChanged)
selectList.setFieldOptions(optionsMap)

// find options for selectlist1
def selectList1 = getFieldByName("Категория")
def customField1 = customFieldManager.getCustomFieldObject(selectList1.fieldId)
def fieldConfig1 = customField1.getRelevantConfig(issueContext)
def options1 = optionsManager.getOptions(fieldConfig1)

// populate values for other fields depending on selectList val
def textField = getFieldByName("Обоснование")
def textField1 = getFieldByName("План внедрения")
def textField2 = getFieldByName("План возврата")
def textField3 = getFieldByName("План тестирования")
def description = getFieldById("description")
def groupPicker = getFieldByName("Группа исполнителей")

def selectListVal = selectList.value as String
def summary = getFieldById("summary")
summary.setFormValue(selectListVal)

switch (selectListVal) {
    case "Добавление / изменение DNS записи":
        selectList1.setFormValue(options1.find { it.value == "DNS сервер(а)" }?.optionId)
        groupPicker.setFormValue("3-я линия поддержки ТС Cloud")
        textField.setFormValue(selectListVal)
        description.setFormValue("""1. Указать основание для изменения ( после создания запроса на изменение связать с задачей, инцидентом, вложить письмо и т.д.)
2. Указать доменное имя (в нашей зоне ответственности или на нашем обслуживании доменное имя)
3. Указать уровень домена (связан с пунктом 2)""")
        textField1.setFormValue("""1. Определить тип записи ANAME, CNAME, MX, spf и так далее (на один адрес их может быть несколько)
2. Определить необходима ли PTR запись (обратная запись, важно для почтовых серверов)
3. Определить IP адрес сервера к которому необходимо назначить доменное имя
4. Оповестить о начале работ службу эксплуатации
5. внести запись в настройки ДНС
6. Принять изменения DNS сервера
7. Проверить принятие и распространение DNS записи на вторичные DNS сервера
8. Подготовить отчет в случае неудачного обновления
9. оповестить службу эксплуатации об окончании работ""")
        textField2.setFormValue("Подготовить отчет в случае неудачного обновления")
        textField3.setFormValue("Проверить принятие и распространение DNS записи на вторичные DNS сервера")
        break
    case "Монтаж оборудования":
        selectList1.setFormValue(options1.find { it.value == "Другое" }?.optionId)
        groupPicker.setFormValue("")
        textField.setFormValue("")
        textField1.setFormValue("")
        textField2.setFormValue("")
        textField3.setFormValue("")
        description.setFormValue("""Стандартный запрос для учета и внесения изменений в CMDB.
В описании изменения указать информацию необходимую для проведения работ.
Также должен быть прикреплен исходный запрос инициатора изменения.""")
        break
    case "Выделение Ethernet портов":
    case "Резервирование Ethernet портов":
    case "Резервирование Ethernet портов CEPH":
    case "Резервирование Ethernet портов Cloudian":
    case "Резервирование Ethernet портов OpenStack":
    case "Резервирование Ethernet портов VMware":
        selectList1.setFormValue(options1.find { it.value == "Сеть" }?.optionId)
        groupPicker.setFormValue("Сетевые инженеры СЭ")
        textField.setFormValue("")
        textField1.setFormValue("")
        textField2.setFormValue("")
        textField3.setFormValue("")
        description.setFormValue("""1. Оборудование, на котором выделяются порты (hostname, расположение)
2. Количество и тип портов
3. Подключаемое оборудование (имя, тип, расположение)
4. Срок резервирования (дата, после которой резерв снимается)
5. Сетевая схема включения (прикрепить файл)
6. Контакты ответственного за резервирование
7. В рамках какого проекта осуществляется резервирование, либо связность с родительской задачей""")
        break
}