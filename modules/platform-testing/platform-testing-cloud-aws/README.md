# platform-testing-cloud-aws

AWS integration-test fixtures based on LocalStack for consumers of Platform Libraries.

The fixture contract and test example are in the [Golden Platform module guide](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-testing-cloud-aws.md).

- [POM and dependencies](pom.xml)
- [Reactor development and verification](../../../README.md)

Integration tests require Docker. LocalStack does not prove production IAM, networking, or AWS service behavior.
