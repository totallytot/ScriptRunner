package jira

import com.atlassian.jira.component.ComponentAccessor
import groovy.time.TimeCategory
import groovy.xml.MarkupBuilder
import com.atlassian.jira.issue.Issue
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean

import java.text.SimpleDateFormat

@WithPlugin("com.riadalabs.jira.plugins.insight")

class ReportGenerator {

    static String generateTechnicalReport(List<Issue> issues) {
        IQLFacade iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.pluginAccessor.classLoader
                .findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade"))
        ObjectFacade objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.pluginAccessor.classLoader
                .findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"))
        def baseurl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        def counter = 1
        builder.html {
            head {
                style("""
                    table {
                        font-size: 12px;
                        border-collapse: collapse;
                        width: 100%;
                    }
                    th {
                        text-align: center;
                        padding: 5px;
                    }
                    td {     
                        text-align: left;        
                        padding: 3px;
                    }
                        th, td { 
                        border: 1px solid black;     
                    }
                    """)
            }
            body {
                p "Справка по оказанию услуг технической поддержки"
                table {
                    tr {
                        //1
                        th "#"
                        //2
                        th "№ заявки Исполнителя"
                        //3
                        th "Наименование Главного управления Банка России"
                        //4
                        th "Отделение Банка России"
                        //5
                        th "Должность, Ф.И.О. ЗАКАЗЧИКА обратившегося в Сервисный Центр"
                        //6
                        th "Дата и время обращения к услуги по технической поддержки"
                        //7
                        th "Тип СИО (ДГУ,ИБП, СКВ)"
                        //8
                        th "Краткое описание обращения по оказанию услуги ТП"
                        //9
                        th "Средства обращения(телефон, факс, эл. почта)"
                        //10
                        th "Описание оказанной услуги СЦ"
                    }
                    issues.each { issue ->
                        tr {
                            //1
                            td counter++
                            //2
                            td { a href: "${baseurl}/browse/${issue.key}", "${issue.key}" }
                            //3
                            def organization = JiraUtilHelper.getCustomFieldValue("Организация", issue) as List
                            if (organization) td organization.first()
                            else td " "
                            //4
                            def unit = JiraUtilHelper.getCustomFieldValue("Подразделение", issue) as List
                            if (unit) td unit.first()
                            else td " "
                            //5
                            def iql = "objectType IN objectTypeAndChildren(Пользователи) AND Профиль = \"${issue.reporter.displayName}\""
                            def insightUser = iqlFacade.findObjects(iql)[0] as ObjectBean
                            if (insightUser) {
                                def jobTitle = objectFacade.loadObjectAttributeBean(insightUser.id, "Должность")
                                        .objectAttributeValueBeans?.first()?.textValue
                                td "${jobTitle}, ${issue.reporter.displayName}"
                            } else td "${issue.reporter.displayName}"
                            //6
                            td "${issue.created.toLocalDateTime()}"
                            //7
                            def service = JiraUtilHelper.getCustomFieldValue("Услуга", issue) as List
                            if (service) td service.first()
                            else td " "
                            //8
                            td "${issue.summary}"
                            //9
                            def requestSource = JiraUtilHelper.getCustomFieldValue("Источник оповещения", issue)
                            if (requestSource) td requestSource.toString()
                            else td " "
                            //10
                            if (issue.description) td issue.description
                            else td " "
                        }
                    }
                }
            }
        }
        return writer.toString()
    }

    static String generateDamageControlReport(List<Issue> issues) {
        ObjectFacade objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.pluginAccessor.classLoader
                .findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"))
        def baseurl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        def counter = 1
        builder.html {
            head {
                style("""
                    table {
                        font-size: 12px;
                        border-collapse: collapse;
                        width: 100%;
                    }
                    th {
                        text-align: center;
                        padding: 5px;
                    }
                    td {     
                        text-align: left;        
                        padding: 3px;
                    }
                        th, td { 
                        border: 1px solid black;     
                    }
                    """)
            }
            body {
                p "Справка по ремонтно-восстановительным работам"
                table {
                    tr {
                        //1
                        th "#"
                        //2
                        th "№ заявки Исполнителя"
                        //3
                        th "Наименование Главного управления Банка России"
                        //4
                        th "Отделение Банка России"
                        //5
                        th "Место расположения СИО (адрес объекта, № помещения)"
                        //6
                        th "Тип СИО (ДГУ, ИБП, СКВ)"
                        //7
                        th "Модель (изготовитель)"
                        //8
                        th "Краткое описание неисправности"
                        //9
                        th "Дата возникновения неисправности у Заказчика"
                        //10
                        th "Дата и время получения заявки Исполнителем (Tп)"
                        //11
                        th "Дата и время устранения (Tу)"
                        //12
                        th "Длительность устранения Тд=Ту-Тп"
                        //13
                        th "Детальное описание неисправности"
                        //14
                        th "Метод устранения(перезагрузка,агрегатный ремонт или ремонт методом замены и т.д.)"
                        //15
                        th "Используемые элементы, узлы и  агрегаты замены "
                    }
                    issues.each { issue ->
                        tr {
                            //1
                            td counter++
                            //2
                            td { a href: "${baseurl}/browse/${issue.key}", "${issue.key}" }
                            //3
                            def organization = JiraUtilHelper.getCustomFieldValue("Организация", issue) as List
                            if (organization) td organization.first()
                            else td " "
                            //4
                            def unit = JiraUtilHelper.getCustomFieldValue("Подразделение", issue) as List
                            if (unit) td unit.first()
                            else td " "
                            //5
                            def equipment = JiraUtilHelper.getCustomFieldValue("КЕ", issue) as List
                            if (equipment) {
                                def insightEquipment = equipment.first() as ObjectBean
                                def location = objectFacade.loadObjectAttributeBean(insightEquipment.id, "Расположение")
                                        .objectAttributeValueBeans?.first()?.textValue
                                td location
                            } else td " "
                            //6
                            def service = JiraUtilHelper.getCustomFieldValue("Услуга", issue) as List
                            if (service) td service.first()
                            else td " "
                            //7
                            if (equipment) {
                                def insightEquipment = equipment.first() as ObjectBean
                                def model = objectFacade.loadObjectAttributeBean(insightEquipment.id, "Модель")
                                            .objectAttributeValueBeans?.first()?.textValue
                                def vendor = objectFacade.loadObjectAttributeBean(insightEquipment.id, "Вендор")
                                            .objectAttributeValueBeans?.first()?.textValue
                                td "${model} ${vendor}"
                            } else td " "
                            //8
                            td "${issue.summary}"
                            //9
                            td " "
                            //10 Тп
                            def formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm")
                            def mailTime = JiraUtilHelper.getCustomFieldValue("Время прихода письма", issue) as Date
                            if (mailTime) td formatter.format(mailTime)
                            else td " "
                            //11 Ту
                            def resolutionDate = issue.resolutionDate?.toLocalDateTime()?.toDate()
                            if (resolutionDate) td formatter.format(resolutionDate)
                            else td " "
                            //12 Ту-Тп
                            if (resolutionDate && mailTime) {
                                def duration = TimeCategory.minus(resolutionDate, mailTime)
                                td "${duration.days} д. ${duration.hours} ч. ${duration.minutes} мин."
                            } else td " "
                            //13
                            if (issue.description) td issue.description
                            else td " "
                            //14
                            if (issue.resolution?.name) td issue.resolution.name
                            else td " "
                            //15
                            def resolutionInfo = JiraUtilHelper.getCustomFieldValue("Примечание к решению", issue)
                            if (resolutionInfo) td resolutionInfo.toString()
                            else td " "
                        }
                    }
                }
            }
        }
        return writer.toString()
    }
}