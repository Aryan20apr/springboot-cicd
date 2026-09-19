# smoke-tests/crud.hurl
# Full CRUD round-trip: create a user, read it back, then delete it.
# This verifies the app can connect to the staging database and process requests end-to-end.

# Step 1 — Create a test user
POST http://{{staging_host}}:8081/api/v1/users
Content-Type: application/json
{
  "name": "smoke-test-user",
  "email": "smoke@test.local"
}
HTTP 201
[Captures]
user_id: jsonpath "$.id"

# Step 2 — Read the created user back by ID
GET http://{{staging_host}}:8081/api/v1/users/{{user_id}}
HTTP 200
[Asserts]
jsonpath "$.id" == {{user_id}}
jsonpath "$.email" == "smoke@test.local"
jsonpath "$.name" == "smoke-test-user"

# Step 3 — Clean up: delete the test user
DELETE http://{{staging_host}}:8081/api/v1/users/{{user_id}}
HTTP 204

