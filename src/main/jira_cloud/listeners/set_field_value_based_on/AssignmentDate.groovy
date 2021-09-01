package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest

import java.text.SimpleDateFormat

final String TRIGGER_DATE_ID = "customfield_12135"
final String TRIGGER_LABEL = "SCC"
final String TRIGGER_ISSUE_TYPE = "Story"
final String PROJECT_KEY = "TESTONB"

def createMultipleSubTasks = { String parentIssueKey, Map summaryDescriptionMapping ->
    summaryDescriptionMapping.each { k, v ->
        def sbCreationReq = createSubTask(PROJECT_KEY, parentIssueKey, k as String, v as String)
        if (sbCreationReq.status != 201) logger.error "Error during sub-task creation: ${sbCreationReq.status} ${sbCreationReq.body}"
    }
}

logger.info "Working with ${issue.key}"
def isStory = issue.fields.issuetype.name == TRIGGER_ISSUE_TYPE
def hasTriggerLabel = issue.fields.labels.any { it == TRIGGER_LABEL }
def wasAssigned = changelog.items.any { Map changeItem -> changeItem.field == "assignee" && changeItem.to }
if (!wasAssigned || !hasTriggerLabel || !isStory) {
    logger.info "Condition not passed"
    return
} else logger.info "Condition passed"

def firstDaySummary = "Day 1 - Getting set up"
def firstDayDesc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Welcome to Labster! This is Jira, which we use to manage our workflow and processes. In here, you will find Onboarding tasks for today ("
                        },
                        {
                            "type": "text",
                            "text": "subtasks",
                            "marks": [
                                {
                                    "type": "code"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " of this "
                        },
                        {
                            "type": "text",
                            "text": "Story",
                            "marks": [
                                {
                                    "type": "code"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": "). Each day of onboarding will have its own Story ticket. Please move it to  "
                        },
                        {
                            "type": "status",
                            "attrs": {
                                "text": "In PROGRESS",
                                "color": "blue",
                                "localId": "39554905-2ddb-42d9-97ef-ee8a3faccd61",
                                "style": ""
                            }
                        },
                        {
                            "type": "text",
                            "text": " when you start working on it and to "
                        },
                        {
                            "type": "status",
                            "attrs": {
                                "text": "DONE",
                                "color": "green",
                                "localId": "39554905-2ddb-42d9-97ef-ee8a3faccd61",
                                "style": ""
                            }
                        },
                        {
                            "type": "text",
                            "text": " when you finish working on it. You can also update the status of the subtasks in the same way. When you start working on simulation development, this too will be managed through Jira, but you will learn much more about that during the week. "
                        }
                    ]
                },
                {
                    "type": "panel",
                    "attrs": {
                        "panelType": "info"
                    },
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Tip! Use the filter "
                                },
                                {
                                    "type": "text",
                                    "text": "Only My Issues",
                                    "marks": [
                                        {
                                            "type": "code"
                                        }
                                    ]
                                },
                                {
                                    "type": "text",
                                    "text": " on the "
                                },
                                {
                                    "type": "text",
                                    "text": "ONB board",
                                    "marks": [
                                        {
                                            "type": "strong"
                                        }
                                    ]
                                },
                                {
                                    "type": "text",
                                    "text": " to see only tasks that are assigned to you. "
                                }
                            ]
                        }
                    ]
                }
            ]
        }
"""
def firstDayIssueCreationReq = createIssue(PROJECT_KEY, TRIGGER_ISSUE_TYPE, firstDaySummary, firstDayDesc,
        issue.fields.assignee.accountId as String)
if (firstDayIssueCreationReq.status != 201)
    logger.error "Error during issue creation request (Day 1 - Getting set up): ${firstDayIssueCreationReq.status}: " +
            "${firstDayIssueCreationReq.body}"
else {
    def parentIssueKey = firstDayIssueCreationReq.body["key"] as String
    def triggerDate = new Date(timestamp)
    def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    def triggerDateVal = formatter.format(triggerDate)
    def setTriggerDateReq = setField(parentIssueKey as String, TRIGGER_DATE_ID, triggerDateVal)
    if (setTriggerDateReq.status != 204) logger.error "Error during Trigger Date update: ${setTriggerDateReq.status}: ${setTriggerDateReq.body}"
    else logger.info "Trigger date was set to ${triggerDateVal} in ${parentIssueKey}"
    def linkIssuesReq = linkIssues(issue.key as String, parentIssueKey as String, "Relates")
    if (linkIssuesReq.status == 201) logger.info "Issues were linked"
    else logger.error "Error during during issue linking ${linkIssuesReq.status} ${linkIssuesReq.body}"
    // sub-task creation
    logger.info "Created issue with trigger date: ${parentIssueKey}"
    def summaryDescriptionMapping = [:]
    def sb1Sum = "Meet your Onboarding Coordinator"
    def sb1Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "This meeting will give you a chance to meet some fellow Scientific Content Creators who will give you a helping hand throughout your onboarding. You will also learn more about how the scientific content circle is structured, get an overview of the weeks ahead, and have an opportunity to ask any burning questions. "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "See the Google Calendar Invite Called Welcome to Labster!"
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb1Sum, sb1Desc)
    def sb2Sum = "Meet the People Circle"
    def sb2Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "In this meeting you will meet the people circle and some of the other new hires at Labster. See the "
                        },
                        {
                            "type": "text",
                            "text": "Google Calendar invite",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://calendar.google.com/calendar"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " and mark this task as done when you have attended this meeting. "
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb2Sum, sb2Desc)
    def sb3Sum = "Set up your PC"
    def sb3Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Set up your PC, naming it according to the "
                        },
                        {
                            "type": "text",
                            "text": "naming convention",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://docs.google.com/document/d/1iQDb1IBfzMGsUuwIqluc_5CbUd01iOxH2sqqhxy-Ok4/edit"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " (or rename it!), download McAfee anti-virus (also explained in the linked document) and set up your Google account. "
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb3Sum, sb3Desc)
    def sb4Sum = "Set up Google Calendar and Google Drive"
    def sb4Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Among other things, we use a shared Labster Calendar to keep track of who is in which office, travelling, on holiday, etc. Please update the calendar whenever you are on a business trips, long term sick leave or are taking vacation."
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "You also have your personal work calendar which you can use for scheduling meetings etc. You have access to everyone’s calendar within Labster so you can always see your colleagues' schedule too. "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "You find the calendar by on to Google, and going to Calendar."
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "If you are not familiar with how Google Calendar works, here is a link on “how to start”:"
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "inlineCard",
                            "attrs": {
                                "url": "https://support.google.com/calendar/answer/2465776?co=GENIE.Platform%3DDesktop&hl=e"
                            }
                        },
                        {
                            "type": "text",
                            "text": " "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "If you have any problems getting access, please reach out to Sejal."
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb4Sum, sb4Desc)
    def sb5Sum = "Start Introduction to Labster Tools Course"
    def sb5Desc = """{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "All courses can be found in the "
                        },
                        {
                            "type": "text",
                            "text": "Labster Hub, via Your Learning Dashboard. ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/plugins/servlet/ac/com.stiltsoft.confluence.quiz/learning"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": "You can access this by going to "
                        },
                        {
                            "type": "text",
                            "text": "confluence",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/spaces/ONBOARDING/overview?mode=global"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": ", clicking on the apps section of the tool bar, and then Learning: "
                        }
                    ]
                },
                {
                    "type": "mediaSingle",
                    "attrs": {
                        "width": 100,
                        "layout": "center"
                    },
                    "content": [
                        {
                            "type": "media",
                            "attrs": {
                                "id": "5b6431a9-0bdb-4cbf-9579-81d62a138000",
                                "type": "file",
                                "collection": "",
                                "width": 755,
                                "height": 447
                            }
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Alternatively all your courses can be accessed "
                        },
                        {
                            "type": "text",
                            "text": "via this link. ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/spaces/ONBOARDING/overview?mode=global"
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "This course is part of the "
                        },
                        {
                            "type": "text",
                            "text": "Essential Tools course.",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://quizzes.stiltsoft.net/course?token=dMaPD3usadQV0y1ArwXy6wr9qmM1CxgiZ8n-Ps9VU2jb5yzGpXH7mLLjC1Va-Jkg"
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "This course contains a few subsections. Start with:"
                        }
                    ]
                },
                {
                    "type": "bulletList",
                    "content": [
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "Our Tools: Introduction to Labster Tools, Overview: Communication Tools, and Slack"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "Other Tools: Overview: Other Tools, LastPass"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "You will revisit the rest of the course later in the week"
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb5Sum, sb5Desc)
    def sb6Sum = "Set Up LastPass"
    def sb6Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Now that you have taken the LastPass course, set up LastPass and two-factor authentication to comply with our data safety standards. You will be able to find a link to set up an enterprise account in your email. "
                        }
                    ]
                }
            ]
        }

"""
    summaryDescriptionMapping.put(sb6Sum, sb6Desc)
    def sb7Sum = "Join Important Slack Channels"
    def sb7Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Slack is what we use for all daily communication. "
                        },
                        {
                            "type": "text",
                            "text": "Complete the Slack course first (part of the Essential Tools course). ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://quizzes.stiltsoft.net/course?token=dMaPD3usadQV0y1ArwXy6wr9qmM1CxgiZ8n-Ps9VU2jb5yzGpXH7mLLjC1Va-Jkg"
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Then install Slack or log in via the browser, and jump straight in. Below is a list of Slack channels that you should join (marked * ), or that we recommend. Feel free to explore and join other channels too using the channel browser function. "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "You can also organise your slack channels into sections to keep your sidebar clean. ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://slack.com/intl/en-gb/help/articles/360043207674-Organise-your-sidebar-with-customised-sections"
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Content",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Academy-content*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Scientific-content*                                                                                                                          "
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Content-tools*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Content-dev-cluster*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Squad-(squadname)*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Content-accessibility*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Simsters (hidden channel)"
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Announcements",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " "
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "General*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Important*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Good-news-and-high-fives"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Feedback"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Labster-time*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Share-channels"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Product-circle*"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Role-marketplace"
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Social Channels",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " "
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Landscapes"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Lobsters-at-home"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Welcome-aboard"
                        },
                        {
                            "type": "hardBreak"
                        },
                        {
                            "type": "text",
                            "text": "Copenhagen (for those based in CPH)"
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb7Sum, sb7Desc)
    def sb8Sum = "Useful Bookmarks"
    def sb8Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Here is a page",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/2737733732/Useful+Pages+to+Bookmark+for+Scientific+Content+Creators"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " with some suggestions of tools, guides, and sites you will use on a daily and weekly basis that would be helpful to bookmark. "
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb8Sum, sb8Desc)
    def sb9Sum = "Start Labster all Onboard Course"
    def sb9Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "All courses can be found in the Labster Hub, via Your Learning Dashboard. You can access this by going to "
                        },
                        {
                            "type": "text",
                            "text": "confluence",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/spaces/ONBOARDING/overview?mode=global"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": ", clicking on the apps section of the tool bar, and then Learning: "
                        }
                    ]
                },
                {
                    "type": "mediaSingle",
                    "attrs": {
                        "width": 100,
                        "layout": "center"
                    },
                    "content": [
                        {
                            "type": "media",
                            "attrs": {
                                "id": "5b6431a9-0bdb-4cbf-9579-81d62a138000",
                                "type": "file",
                                "collection": "",
                                "width": 755,
                                "height": 447
                            }
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Alternatively all your courses can be accessed "
                        },
                        {
                            "type": "text",
                            "text": "via this link. ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/spaces/ONBOARDING/overview?mode=global"
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Start with:"
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Welcome to Labster",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Our Vision, Mission and Goals",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "You will revisit the rest of the course later in the week."
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb9Sum, sb9Desc)
    def sb10Sum = "Intro to the Content Circle Course"
    def sb10Desc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "All courses can be found in the "
                        },
                        {
                            "type": "text",
                            "text": "Labster Hub, via Your Learning Dashboard",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/plugins/servlet/ac/com.stiltsoft.confluence.quiz/learning"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": ". This course is part of the "
                        },
                        {
                            "type": "text",
                            "text": "Content Circle course. ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://quizzes.stiltsoft.net/course?token=gQUbUwFkCQ9WzCIXhuHK8fdY6Rhp6au3Zd9hYnZTbQN_vZ8jXm9vcmOodPeJTKTo"
                                    }
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Start with the "
                        },
                        {
                            "type": "text",
                            "text": "Intro to the Content Circle",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " section, we will revist the rest of this course later in the week. "
                        }
                    ]
                }
            ]
        }
"""
    summaryDescriptionMapping.put(sb10Sum, sb10Desc)
    createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
}

def additionalIssueDesc = """
{
            "version": 1,
            "type": "doc",
            "content": [
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Over the course of your first week play 5 simulations, some suggestions are listed below. You can access our SIM catalogue using "
                        },
                        {
                            "type": "text",
                            "text": "this link ",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://api2.labster.com/"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": "click on the "
                        },
                        {
                            "type": "text",
                            "text": "Filter ",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": "button to search for sims using the three letter code, or keywords. Alternatively, you can search for topics with the "
                        },
                        {
                            "type": "text",
                            "text": "Filter",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " button, and play any simulation that takes your fancy. "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Make sure to play at least 1 in accessibility mode before your Accessibility at Labster session. In this mode, alt text is read out for images and visual tasks and you can use the keyboard to navigate. You can play any simulation that has been made accessible (marked * ) in accessibility mode by adding "
                        },
                        {
                            "type": "text",
                            "text": "?accessibility=true",
                            "marks": [
                                {
                                    "type": "code"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " to the end of the standard URL (see "
                        },
                        {
                            "type": "text",
                            "text": "STEP ONE",
                            "marks": [
                                {
                                    "type": "strong"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " on "
                        },
                        {
                            "type": "text",
                            "text": "this page",
                            "marks": [
                                {
                                    "type": "link",
                                    "attrs": {
                                        "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/1430782113/Accessibility+Workflow+from+Scientific+Content+Creator+SCC+Perspective"
                                    }
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": "). "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Under Simulation Excellence you will find simulations that have recently been split up into short "
                        },
                        {
                            "type": "text",
                            "text": "Mini",
                            "marks": [
                                {
                                    "type": "code"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "text": " simulations and updated. Compare the two. "
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Recent simulations"
                        }
                    ]
                },
                {
                    "type": "orderedList",
                    "content": [
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "CRY - Recrystallization: Purify your solid"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "GMP - Aseptic Technique: Culture your sample without contamination*"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "BAS - Bacterial Cell Structures: An introduction to the bacterial cell*"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "TLC - Thin Layer Chromatography*"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "EVA - Evolution: Founding theories and principles*"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "ETS - Biomes: Identify and create the main biomes on Earth"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "ELE - Basic Electricity: Understand how electricity works"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "COC - Organic Chemistry Introduction: Learn about organic compounds"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "LVL - Trophic Levels: Grazer vs. predator"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "HST - Homeostatic Control: How does the human body keep itself in balance?"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "paragraph",
                    "content": [
                        {
                            "type": "text",
                            "text": "Simulation Excellence "
                        }
                    ]
                },
                {
                    "type": "orderedList",
                    "content": [
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "Compare FER - Fermentation, with FEM - Fermentation: Optimize bio-ethanol production"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "listItem",
                            "content": [
                                {
                                    "type": "paragraph",
                                    "content": [
                                        {
                                            "type": "text",
                                            "text": "Compare MIC - Microscopy, with BLM - Basic Light microscopy"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        }
"""
def additionalIssueCreationReq = createIssue(PROJECT_KEY, TRIGGER_ISSUE_TYPE, "Play 5 simulations", additionalIssueDesc, issue.fields.assignee.accountId as String)
if (additionalIssueCreationReq.status != 201)
    logger.error "Error during issue creation request (Play 5 simulations): ${additionalIssueCreationReq.status}: ${additionalIssueCreationReq.body}"
else {
    def createdIssueKey = additionalIssueCreationReq.body.key
    logger.info "Created issue: ${createdIssueKey}"
    def linkIssuesReq = linkIssues(issue.key as String, createdIssueKey as String, "Relates")
    if (linkIssuesReq.status == 201) logger.info "Issues were linked"
    else logger.error "Error during during issue linking ${linkIssuesReq.status} ${linkIssuesReq.body}"
}


static def setField(String issueKey, String customfieldId, String value) {
    Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [(customfieldId): value]]).asString()
}

static def createIssue(String projectKey, String issueTypeName, String summary, String description, String assigneeId) {
    def body = """
{ 
    "fields": {
        "project": {
            "key": "${projectKey}"
        },
        "issuetype": {
            "name": "${issueTypeName}"
        },
        "summary": "${summary}",
        "assignee": {
            "id": "${assigneeId}"
        },
        "labels": [
            "SCC_Onboarding"
        ],
        "description": ${description}
    }
}
"""
    Unirest.post("/rest/api/3/issue")
            .header("Content-Type", "application/json")
            .body(body).asObject(Map)
}

static def createSubTask(String projectKey, String parentKey, String summary, String description) {
    def body = """
{ 
    "fields": {
        "project": {
            "key": "${projectKey}"
        },
        "issuetype": {
            "name": "Sub-task"
        },
        "parent": {
            "key": "${parentKey}"
        },
        "summary": "${summary}",
        "description": ${description}
    }
}
"""
    Unirest.post("/rest/api/3/issue")
            .header("Content-Type", "application/json")
            .body(body)
            .asObject(Map)
}

static def linkIssues(String sourceIssueKey, String targetIssueKey, String linkType) {
    Unirest.post('/rest/api/3/issueLink')
            .header('Content-Type', 'application/json')
            .body([type        : [name: linkType],
                   outwardIssue: [key: sourceIssueKey],
                   inwardIssue : [key: targetIssueKey]
            ])
            .asString()
}