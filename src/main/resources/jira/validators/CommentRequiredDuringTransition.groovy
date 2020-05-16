package jira.validators

import com.opensymphony.workflow.InvalidInputException

if (!transientVars.comment) throw new InvalidInputException ("Комментарий обязателен!")