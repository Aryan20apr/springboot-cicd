# smoke-tests/info.hurl
# Verifies that the deployed image was built from the exact same commit that
# triggered this pipeline run. The expected_sha variable is injected by the
# workflow: --variable expected_sha=${{ github.sha }}
GET http://{{staging_host}}:8081/actuator/info
HTTP 200
[Asserts]
jsonpath "$.git.commit.id.full" == "{{expected_sha}}"
