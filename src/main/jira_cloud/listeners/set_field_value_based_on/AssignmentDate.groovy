package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest

import java.text.SimpleDateFormat

final String TRIGGER_DATE_ID = "customfield_12135"
final String TRIGGER_LABEL = "SCC"
final String TRIGGER_ISSUE_TYPE = "Story"

logger.info "Working with ${issue.key}"
def isStory = issue.fields.issuetype.name == TRIGGER_ISSUE_TYPE
def hasTriggerLabel = issue.fields.labels.any { it == TRIGGER_LABEL }
def wasAssigned = changelog.items.any { Map changeItem -> changeItem.field == "assignee" && changeItem.to }
if (!wasAssigned || !hasTriggerLabel || !isStory) {
    logger.info "Condition not passed"
    return
} else
    logger.info "Condition passed"

def triggerDate = new Date(timestamp)
def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
def triggerDateVal = formatter.format(triggerDate)
def setTriggerDateReq = setField(issue.key as String, TRIGGER_DATE_ID, triggerDateVal)
if (setTriggerDateReq.status != 204)
    logger.error "Error during Trigger Date update: ${setTriggerDateReq.status}: ${setTriggerDateReq.body}"
else
    logger.info "Trigger date was set to ${triggerDateVal} in ${issue.key}"
def desc = """
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Over the course of your first week play 5 of the simulations listed below. You can access our SIM catalouge using "
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
          "text": "button to search for sims using the three letter code, or keywords. "
        }
      ]
    },
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Make sure to play at least 1 in accessibility mode before Thursday. In this mode, alt text is read out for images and visual tasks and you can use the keyboard to navigate. You can play any simulation that has been made accessible (marked * ) in accessibility mode by adding "
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
          "text": " to the end of the standard URL (see STEP ONE on "
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
          "text": " simulations and updated. Compare the two to "
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
                  "text": "TLC - Thin Layer Chromatography"
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
                  "text": "Compare FER - Fermentation with FEM - Fermentation: Optimize bio-ethanol production"
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
                  "text": "Compare MIC - Microscopy with BLM - Basic Light microscopy"
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
def issueCreationReq = createIssue("TESTONB", "Story", "Play 5 simulations", desc, issue.fields.assignee.accountId as String)
if (issueCreationReq.status != 201)
    logger.error "Error during issue creation request: ${issueCreationReq.status}: ${issueCreationReq.body}"
else
    logger.info "Created issue: ${issueCreationReq.body.key}"

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