# smoke-tests/health.hurl
# Verifies that the staging app is up and reports a healthy status.
GET http://{{staging_host}}:8081/actuator/health
HTTP 200
[Asserts]
jsonpath "$.status" == "UP"
