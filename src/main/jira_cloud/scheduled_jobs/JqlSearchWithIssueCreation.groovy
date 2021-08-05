package jira_cloud.scheduled_jobs

import groovy.time.TimeCategory
import groovy.transform.Field
import kong.unirest.Unirest

import java.text.SimpleDateFormat

@Field final String TRIGGER_DATE_ID = "customfield_12135"
final String PROJECT_KEY = "TESTONB"
final String ISSUE_TYPE_NAME = "Story"
final String JQL = """project = ${PROJECT_KEY} and issuetype = ${ISSUE_TYPE_NAME} and "Trigger Date[Time stamp]" is not empty"""
final int BASIC_CREATION_PERIOD_HOURS = 23 // should be less than 24 or add days val
final int ADDITIONAL_CREATION_PERIOD_HOURS = 1
final String TRIGGER_LABEL = "SCC"

logger.info "Scheduled job start"
def searchResult = executeSearch(JQL, 0, 100) as List
if (searchResult.empty) {
    logger.info "Search result is empty"
    return
}
logger.info "Search size: ${searchResult.size()}"

def executeBasicScheduledActions = { String triggerIssueKey, String summary, String description, String assigneeId, Boolean setTriggerDate ->
    def issueCreationReq = createScheduledIssue(PROJECT_KEY, ISSUE_TYPE_NAME, summary, description, assigneeId, setTriggerDate)
    def createdIssueKey = null
    if (issueCreationReq.status == 201) {
        createdIssueKey = issueCreationReq.body["key"] as String
        logger.info "Created issue key ${createdIssueKey}"
        def clearTriggerDateReq = setField(triggerIssueKey, TRIGGER_DATE_ID, null)
        if (clearTriggerDateReq.status == 204) logger.info "Trigger date was removed in ${triggerIssueKey}"
        else logger.error "Error during trigger date removal: ${clearTriggerDateReq.status} ${clearTriggerDateReq.body}"
        def linkIssuesReq = linkIssues(triggerIssueKey, createdIssueKey, "Relates")
        if (linkIssuesReq.status == 201) logger.info "Issues were linked"
        else logger.error "Error during during issue linking ${linkIssuesReq.status} ${linkIssuesReq.body}"
    } else logger.error "Error during issue creation: ${issueCreationReq.status} ${issueCreationReq.body}"
    return createdIssueKey
}

def createMultipleSubTasks = { String parentIssueKey, Map summaryDescriptionMapping ->
    summaryDescriptionMapping.each { k, v ->
        def sbCreationReq = createSubTask(PROJECT_KEY, parentIssueKey, k as String, v as String)
        if (sbCreationReq.status != 201) logger.error "Error during sub-task creation: ${sbCreationReq.status} ${sbCreationReq.body}"
    }
}

searchResult.each { Map issue ->
    logger.info "Working with ${issue.key}"
    def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    def triggerDate = formatter.parse(issue.fields[TRIGGER_DATE_ID] as String)
    def currentDate = new Date()
    def difference = currentDate.time - triggerDate.time
    def deltaHours = Math.round(difference/3600000)
    logger.info "deltaHours: ${deltaHours}"

    def summary = issue.fields.summary
    def assigneeId = issue.fields.assignee?.accountId as String

    def day2Summary = "Day 2 - Technical software"
    def day3Summary = "Day 3 - Builder time"
    def day4Summary = "Day 4 - Accessibility & Writing"
    def day5Summary = "Day 5 - Wrap up the week"
    def week2Summary = "Week 2 - Simulation quality and general good practice"

    if (deltaHours >= BASIC_CREATION_PERIOD_HOURS) {
        def hasTriggerLabel = issue.fields.labels.any { it == TRIGGER_LABEL }
        if (hasTriggerLabel && assigneeId) {
            def day2Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "You have made it through day 1! Today we will be introducing you to some of our technical software, how to set it up and how to use it. Please start this day with the Install Git for Windows, Sourcetree, and Sublime so you can find and hopefully solve any access issues before your session with "
        },
        {
          "type": "mention",
          "attrs": {
            "id": "5a72ebef3df3a02be346090a",
            "text": "Francisco Moreno",
            "accessLevel": ""
          }
        },
        {
          "type": "text",
          "text": " in the afternoon."
        },
        {
          "type": "hardBreak"
        },
        {
          "type": "hardBreak"
        },
        {
          "type": "text",
          "text": "We’ve also thrown in some organizational tools like Glassfrog, Cezanne and 7Geese, but the courses will cover all of these."
        }
      ]
    }
  ]
}
"""
            def parentIssueKey = executeBasicScheduledActions(issue.key as String, day2Summary, day2Desc, assigneeId, Boolean.TRUE)
            if (parentIssueKey) {
                def summaryDescriptionMapping = [:]
                def sb1Sum = "Content General Knowledge course"
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
          "text": "All courses can be found in the"
        },
        {
          "type": "text",
          "text": " Labster Hub, via Your Learning Dashboard. ",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/plugins/servlet/ac/com.stiltsoft.confluence.quiz/learning#!participant-dashboard"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": "This course is the second part of the "
        },
        {
          "type": "text",
          "text": "Content Circle course",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://quizzes.stiltsoft.net/course?token=gQUbUwFkCQ9WzCIXhuHK8fdY6Rhp6au3Zd9hYnZTbQN_vZ8jXm9vcmOodPeJTKTo"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": " you started yesterday. "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb1Sum, sb1Desc)
                def sb2Sum = "Install Git for Windows, Sourcetree, Sublime"
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
          "text": "First, install "
        },
        {
          "type": "text",
          "text": "Git for Windows",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://github.com/git-for-windows/git/releases/download/v2.30.0.windows.2/Git-2.30.0.2-64-bit.exe"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": " (latest version), then watch this "
        },
        {
          "type": "text",
          "text": "video ",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://www.loom.com/share/253438762706431a87f00f9351792463"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": "about installing Sourcetree and download the rest of the software:"
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
                  "text": "Sourcetree",
                  "marks": [
                    {
                      "type": "link",
                      "attrs": {
                        "href": "https://www.sourcetreeapp.com/"
                      }
                    }
                  ]
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
                  "text": "Sublime",
                  "marks": [
                    {
                      "type": "link",
                      "attrs": {
                        "href": "https://www.sublimetext.com/"
                      }
                    }
                  ]
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
                summaryDescriptionMapping.put(sb2Sum, sb2Desc)
                def sb3Sum = "How to use Git, Sourcetree, and Sublime (meeting)"
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
          "text": "See the Google Calendar invite, please make sure you have downloaded these programs before the meeting. This is to troubleshoot any issues and introduce you to these tools. "
        }
      ]
    }
  ]
}"""
                summaryDescriptionMapping.put(sb3Sum, sb3Desc)
                def sb4Sum = "Finish Essential Tools course"
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
          "text": "In the "
        },
        {
          "type": "text",
          "text": "Labster Learning Hub",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/plugins/servlet/ac/com.stiltsoft.confluence.quiz/learning#!creator-dashboard"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": " you will find the "
        },
        {
          "type": "text",
          "text": "Essential Tools course",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://quizzes.stiltsoft.net/course?token=dMaPD3usadQV0y1ArwXy6wr9qmM1CxgiZ8n-Ps9VU2jb5yzGpXH7mLLjC1Va-Jkg"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": " and complete the courses that you did not yet do:"
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
                  "text": "Cezanne"
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
                  "text": "G-suite"
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
                  "text": "Glassfrog"
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
                  "text": "Paycor"
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
          "text": "and take the Quiz: Labster Tool to prove your mastery!"
        },
        {
          "type": "hardBreak"
        },
        {
          "type": "hardBreak"
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb4Sum, sb4Desc)
                def sb5Sum = "Access and tools check"
                def sb5Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "In addition to Google, please make sure that you have downloaded and have access to:"
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
                  "text": "Sublime",
                  "marks": [
                    {
                      "type": "link",
                      "attrs": {
                        "href": "https://download.sublimetext.com/Sublime%20Text%20Build%203211%20x64%20Setup.exe"
                      }
                    }
                  ]
                },
                {
                  "type": "text",
                  "text": " → XML editor"
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
                  "text": "SourceTree",
                  "marks": [
                    {
                      "type": "link",
                      "attrs": {
                        "href": "https://product-downloads.atlassian.com/software/sourcetree/windows/ga/SourceTreeSetup-3.3.9.exe"
                      }
                    }
                  ]
                },
                {
                  "type": "text",
                  "text": " → Get access to our repositories"
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
                  "text": "Grammarly",
                  "marks": [
                    {
                      "type": "link",
                      "attrs": {
                        "href": "https://download-editor.grammarly.com/windows/GrammarlySetup.exe"
                      }
                    }
                  ]
                },
                {
                  "type": "text",
                  "text": " or add the Chrome extension → Grammar checker"
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
                  "text": "Loom",
                  "marks": [
                    {
                      "type": "link",
                      "attrs": {
                        "href": "https://cdn.loom.com/desktop-packages/Loom%20Setup%200.57.0.exe"
                      }
                    }
                  ]
                },
                {
                  "type": "text",
                  "text": " → Create screencasts"
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
          "text": "You will cover Sublime and Sourcetree in the session with Fran today. Grammarly and Loom are pretty intuitive."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb5Sum, sb5Desc)
                def sb6Sum = "Finish All On-Board course"
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
          "text": "Complete the"
        },
        {
          "type": "text",
          "text": " All On-board course",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://quizzes.stiltsoft.net/course?token=x42yB8UhcZ_ng0cmuqAwjnJBGZDGB_hqpU-3rG9nVCbZkwxFQVaiXfwB5moFWqWy"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": " that you started yesterday with the sections: "
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
                  "text": "Our Values"
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
                  "text": "How We Work"
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
                summaryDescriptionMapping.put(sb6Sum, sb6Desc)
                createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
            }
        } else if (summary == day2Summary && assigneeId) {
            def day3Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "You know the drill by now! Tasks for day 3, you will be learning to use the Builder, finding out what Content Squads do, and start getting to know Jira. Today you will also be joining our regular TAC meeting."
        }
      ]
    }
  ]
}
"""
            def parentIssueKey = executeBasicScheduledActions(issue.key as String, day3Summary, day3Desc, assigneeId, Boolean.TRUE)
            if (parentIssueKey) {
                def summaryDescriptionMapping = [:]
                def sb1Sum = "Introduction to Builder (meeting)"
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
          "text": "You know the drill by now! Tasks for day 3, you will be learning to use the Builder, finding out what Content Squads do, and start getting to know Jira. Today you will also be joining our regular TAC meeting."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb1Sum, sb1Desc)
                def sb2Sum = "Practise using Builder and saving to Git"
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
          "text": "Check out the Onboarding Builder Fun Story and practice using the Builder and saving to Git. You will have time for this during the rest of the week too. "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb2Sum, sb2Desc)
                def sb3Sum = "Simulation Development Workflow Course"
                def sb3Desc = null
                summaryDescriptionMapping.put(sb3Sum, sb3Desc)
                def sb4Sum = "Introduce yourself in #welcome-aboard"
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
          "text": "If you have not yet done so, introduce yourself to the company on Slack in #welcome-aboard! This helps people put a face to a name and you might find kindred spirits."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb4Sum, sb4Desc)
                createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
            }
        } else if (summary == day3Summary && assigneeId) {
            def day4Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "You know our tools, you know our simulations, let’s look at making them accessible and good writing today!"
        }
      ]
    }
  ]
}
"""
            def parentIssueKey = executeBasicScheduledActions(issue.key as String, day4Summary, day4Desc, assigneeId, Boolean.TRUE)
            if (parentIssueKey) {
                def summaryDescriptionMapping = [:]
                def sb1Sum = "Jira Basic course"
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
          "text": "Complete the Jira Basic course in the Labster Hub."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb1Sum, sb1Desc)
                def sb2Sum = "Intro to accessibility (meeting)"
                def sb2Desc = null
                summaryDescriptionMapping.put(sb2Sum, sb2Desc)
                def sb3Sum = "Jira at Labster (meeting)"
                def sb3Desc = null
                summaryDescriptionMapping.put(sb3Sum, sb3Desc)
                def sb4Sum = "Writing at Labster"
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
          "text": "Please read the following Confluence pages:"
        }
      ]
    },
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Writing style guide for Labster simulations",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/79966474/Writing+style+guide+for+Labster+simulations"
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
          "text": "Quiz questions",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://liv-it.atlassian.net/wiki/spaces/SD/pages/369066020/Quiz+Questions"
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
          "text": "Guidelines for writing quiz questions",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://liv-it.atlassian.net/wiki/spaces/SD/pages/598835330/Guidelines+for+Writing+Quiz+Questions"
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
          "text": "Engagement checklist",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://liv-it.atlassian.net/wiki/spaces/SD/pages/601948294/Engagement+Checklist"
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
          "text": "Cultural sensitivity guidelines",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/636879177/Cultural+sensitivity+guidelines"
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
          "text": "Create Theory pages in wiki",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/79962201/6.+Create+Theory+pages+in+wiki"
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
          "text": "Rules for writing theory pages",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/226787337/Rules+for+writing+theory+pages"
              }
            }
          ]
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb4Sum, sb4Desc)
                def sb5Sum = "LastPass Security check and 2FA"
                def sb5Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "You should now have access to LastPass Enterprise. Please check out the Security Dashboard within your LastPass vault. Change weak or reused passwords and ensure that you have two-factor authentication turned on. Also, set up a backup method for two-factor authentication. Take a screenshot of your security score and add it as a comment."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb5Sum, sb5Desc)
                createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
            }
        } else if (summary == day4Summary && assigneeId) {
            def day5Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "You’ve nearly made it through week 1! Today you will find out how accessibility works in practice. Today you will also be joining the Content Dunch (a quarterly catch up for all of Content where we look back at what we have achieved and ahead to what is coming up and generally hang out) and the monthly Labster All-Hands (a company wide presentation of our goals). "
        }
      ]
    }
  ]
}
"""
            def parentIssueKey = executeBasicScheduledActions(issue.key as String, day5Summary, day5Desc, assigneeId, Boolean.TRUE)
            if (parentIssueKey) {
                def summaryDescriptionMapping = [:]
                def sb1Sum = "Content Squads course"
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
          "text": "Using the links in your email, go to the Content circle course complete the How we work together in Squads course."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb1Sum, sb1Desc)
                def sb2Sum = "Wrap up unfinished business from week 1"
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
          "text": "Finish any tasks you did not get round to including playing your first 5 simulations."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb2Sum, sb2Desc)
                def sb3Sum = "Practise with accessibility features in the Builder"
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
          "text": "Using the links in your email, go to the Content circle course and complete the What is the Builder? course."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb3Sum, sb3Desc)
                createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
            }
        } else if (summary == day5Summary && assigneeId) {
            def week2Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Please note that this is a list of tasks for all of week 2 (check out the due date!). It’s up to you when you complete these tasks alongside your first SCC output. "
        }
      ]
    }
  ]
}
"""
            def parentIssueKey = executeBasicScheduledActions(issue.key as String, week2Summary, week2Desc, assigneeId, Boolean.FALSE)
            if (parentIssueKey) {
                def summaryDescriptionMapping = [:]
                def sb1Sum = "How to perform scientific maintenance (Confluence)"
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
          "text": "Familiarise yourself with maintenance at Labster via "
        },
        {
          "type": "text",
          "text": "Confluence",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://labster.atlassian.net/wiki/spaces/SD/pages/97648641/How+to+perform+scientific+maintenance"
              }
            }
          ]
        },
        {
          "type": "text",
          "text": ". "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb1Sum, sb1Desc)
                def sb2Sum = "Squad Focus course"
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
          "text": "Complete the Squad Focus course, part of the Content Circle course"
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb2Sum, sb2Desc)
                def sb3Sum = "Information Security course"
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
          "text": "This course is for everyone, not just new hires. You may have received an email about it. Please contact Quyen if you do not yet have access. She is based in the US and may not respond straight away."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb3Sum, sb3Desc)
                def sb4Sum = "Content + courses"
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
          "text": "Complete the Content + courses in the Content Circle course on the Labster Hub."
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb4Sum, sb4Desc)
                createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
            }
        }
    } else
        logger.info "Skipping basic scheduled actions as deltaHours (${deltaHours}) < CREATION_PERIOD_HOURS (${BASIC_CREATION_PERIOD_HOURS})"

    // Additional ticket to be assigned 4 hrs after Day 3
    if (deltaHours >= ADDITIONAL_CREATION_PERIOD_HOURS && summary == day3Summary && assigneeId) {
        def day3Sum2 = "Onboarding Builder Fun"
        def inwardLinkedIssues = issue.fields.issuelinks.inwardIssue.fields.summary as List
        if (!(day3Sum2 in inwardLinkedIssues)) {
            logger.info "There is no additional linked issue for day 3 in ${issue.key}"
            def day3Desc2 = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "We created a list of tasks and a personal playground for you in the Builder that will help you learn all the essential "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":hammer_pick:",
            "id": "2692",
            "text": "⚒"
          }
        },
        {
          "type": "text",
          "text": " skills "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":slight_smile:",
            "id": "1f642",
            "text": "🙂"
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
          "text": "You have 7 subtasks in this story, that you can also find under this confluence page:"
        },
        {
          "type": "inlineCard",
          "attrs": {
            "url": "https://labster.atlassian.net/wiki/spaces/SD/pages/2558394553/Onboarding+Builder+Learning+Template"
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
          "text": "Feel free to reach out to the POCs ("
        },
        {
          "type": "mention",
          "attrs": {
            "id": "6012ba878fb6ea014a829811",
            "text": "Lisa Bottomley",
            "accessLevel": ""
          }
        },
        {
          "type": "text",
          "text": " "
        },
        {
          "type": "mention",
          "attrs": {
            "id": "5fb69c6c7cc10300692e8d7c",
            "text": "Simran Kaur Cheema",
            "accessLevel": ""
          }
        },
        {
          "type": "text",
          "text": "  "
        },
        {
          "type": "mention",
          "attrs": {
            "id": "6012db88f564b600711a4a9f",
            "text": "Zsuzsanna Csontos",
            "accessLevel": ""
          }
        },
        {
          "type": "text",
          "text": ") of the template if a task is unclear or if you’re stuck in the progress "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":beetle:",
            "id": "1f41e",
            "text": "🐞"
          }
        },
        {
          "type": "text",
          "text": " "
        }
      ]
    }
  ]
}
"""
            def issueCreationReq = createScheduledIssue(PROJECT_KEY, ISSUE_TYPE_NAME, day3Sum2, day3Desc2, assigneeId, Boolean.FALSE)
            if (issueCreationReq.status == 201) {
                def parentIssueKey = issueCreationReq.body["key"] as String
                logger.info "Created additional issue key ${parentIssueKey}"
                def linkIssuesReq = linkIssues(issue.key as String, parentIssueKey, "Relates")
                if (linkIssuesReq.status == 201) logger.info "Issues were linked"
                else logger.error "Error during during issue linking ${linkIssuesReq.status} ${linkIssuesReq.body}"

                def summaryDescriptionMapping = [:]
                def sb1Sum = "1. Play 'Welcome to Builder' simulation"
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
          "text": "Your first task! "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":dizzy:",
            "id": "1f4ab",
            "text": "💫"
          }
        },
        {
          "type": "text",
          "text": " Go to this page and follow the instructions to get started: "
        },
        {
          "type": "inlineCard",
          "attrs": {
            "url": "https://labster.atlassian.net/wiki/spaces/SD/pages/2558394553/Onboarding+Builder+Learning+Template"
          }
        },
        {
          "type": "text",
          "text": " "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb1Sum, sb1Desc)
                def sb2Sum = "2. Explore your own personal Builder playground"
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
          "text": "Time to have access to your own Builder playground by following the instructions here: "
        },
        {
          "type": "inlineCard",
          "attrs": {
            "url": "https://labster.atlassian.net/wiki/spaces/SD/pages/2558394553/Onboarding+Builder+Learning+Template"
          }
        },
        {
          "type": "text",
          "text": " "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":hammer_pick:",
            "id": "2692",
            "text": "⚒"
          }
        },
        {
          "type": "text",
          "text": " "
        },
        {
          "type": "hardBreak"
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb2Sum, sb2Desc)
                def sb3Sum = "3. Conversations and quiz questions in the Builder"
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
          "text": "It’s time for your first modifications in the Builder!!"
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":lobster:",
            "id": "1f99e",
            "text": "🦞"
          }
        },
        {
          "type": "text",
          "text": " "
        },
        {
          "type": "inlineCard",
          "attrs": {
            "url": "https://labster.atlassian.net/wiki/spaces/SD/pages/2558394553/Onboarding+Builder+Learning+Template"
          }
        },
        {
          "type": "text",
          "text": " You can see all the tasks here and try your hand at changing quiz questions and conversations!  "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb3Sum, sb3Desc)
                def sb4Sum = "4. Images, theory pages and GUI screens"
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
          "text": "Adding images or theory pages is not as complicated as it seems! Follow this guide and complete subtasks in your OBT_yourname simulation! "
        }
      ]
    },
    {
      "type": "paragraph",
      "content": [
        {
          "type": "inlineCard",
          "attrs": {
            "url": "https://labster.atlassian.net/wiki/spaces/SD/pages/2558394553/Onboarding+Builder+Learning+Template"
          }
        },
        {
          "type": "text",
          "text": " "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb4Sum, sb4Desc)
                def sb5Sum = "5. Assets and tooltips"
                def sb5Desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Assets are the building blocks of our simulations. Learn how to add them and describe them by tooltips! "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":writing_hand:",
            "id": "270d",
            "text": "✍"
          }
        },
        {
          "type": "text",
          "text": "  "
        },
        {
          "type": "inlineCard",
          "attrs": {
            "url": "https://labster.atlassian.net/wiki/spaces/SD/pages/2558394553/Onboarding+Builder+Learning+Template"
          }
        },
        {
          "type": "text",
          "text": " "
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb5Sum, sb5Desc)
                def sb6Sum = "6. Accessibility and playtime in the Builder"
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
          "text": "This subtask will introduce you to the accessibility features in the Builder and you will have time to decorate the lab and to test your new skills with more complicated tasks! "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":face_with_monocle:",
            "id": "1f9d0",
            "text": "🧐"
          }
        },
        {
          "type": "text",
          "text": " "
        }
      ]
    }
  ]
}

"""
                summaryDescriptionMapping.put(sb6Sum, sb6Desc)
                def sb7Sum = "7. Feedback on this learning experience"
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
          "text": "Please fill out this form to give us feedback on this Builder learning experience "
        },
        {
          "type": "emoji",
          "attrs": {
            "shortName": ":blush:",
            "id": "1f60a",
            "text": "😊"
          }
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
          "text": "Form: "
        },
        {
          "type": "text",
          "text": "https://forms.gle/BijerTEY7C3ts1oZ8",
          "marks": [
            {
              "type": "link",
              "attrs": {
                "href": "https://forms.gle/BijerTEY7C3ts1oZ8"
              }
            }
          ]
        }
      ]
    }
  ]
}
"""
                summaryDescriptionMapping.put(sb7Sum, sb7Desc)
                createMultipleSubTasks(parentIssueKey, summaryDescriptionMapping)
            } else logger.error "Error during additional issue creation: ${issueCreationReq.status} ${issueCreationReq.body}"
        } else logger.info "Additional linked issue for day 3 alreaxdy exists in ${issue.key}"
    }
}

static List executeSearch(String jqlQuery, int startAt, int maxResults) {
    def searchRequest = Unirest.get("/rest/api/3/search")
            .queryString("jql", jqlQuery)
            .queryString("startAt", startAt)
            .queryString("maxResults", maxResults)
            .asObject(Map)
    //noinspection GroovyConditional
    searchRequest.status == 200 ? searchRequest.body.issues as List<Map> : []
}

def createScheduledIssue(String projectKey, String issueTypeName, String summary, String description, String assigneeId, Boolean setTriggerDate) {
    def triggerDateVal = null
    if (setTriggerDate) {
        def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        triggerDateVal = formatter.format(new Date())
    }
    def bodyTriggerDate = """
{ 
    "fields": {
        "project": {
            "key": "${projectKey}"
        },
        "issuetype": {
            "name": "${issueTypeName}"
        },
        "summary": "${summary}",
        "${TRIGGER_DATE_ID}": "${triggerDateVal}",
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
    def bodyBasic = """
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
    def body = setTriggerDate ? bodyTriggerDate : bodyBasic
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

static def setField(String issueKey, String customFieldId, String value) {
    Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [(customFieldId): value]]).asString()
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