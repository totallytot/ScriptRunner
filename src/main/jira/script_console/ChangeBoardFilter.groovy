package jira.script_console

import com.atlassian.greenhopper.manager.rapidview.RapidViewManager
import com.atlassian.greenhopper.model.rapid.RapidView
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean

//shoud be tested on JIRA 7
long BOARD_ID = 0L //board that was bugged
long FILTER_Id = 0L //new filter for that board

@JiraAgileBean
RapidViewManager rapidViewManager

RapidView rapidView = rapidViewManager.get(BOARD_ID).get()
rapidViewManager.update(new RapidView.RapidViewBuilder(rapidView).savedFilterId(FILTER_Id).build())