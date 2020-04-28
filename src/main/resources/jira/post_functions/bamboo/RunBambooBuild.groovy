package jira.post_functions.bamboo

def url = new URL('http:// урл бамбу/rest/api/latest/queue/ключ плана')
HttpURLConnection connection = (HttpURLConnection) url.openConnection()
connection.setRequestMethod('POST')
connection.setRequestProperty("Authorization", "Basic ")
connection.getResponseCode()