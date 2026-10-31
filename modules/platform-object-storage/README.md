# Platform Object Storage

Provider-neutral AWS S3 and Azure Blob Storage capability.

The public contract, configuration, errors, examples, and provider limitations are documented in the [Golden Platform module guide](https://github.com/BrunoBS/golden-platform/blob/docs/golden-platform-foundation/docs/libraries/modules/platform-object-storage.md).

- [POM and dependencies](pom.xml)
- [Reactor development and verification](../../README.md)

Integration tests require Docker and use LocalStack for AWS and Azurite for Azure-compatible storage operations. Azure user delegation SAS generation requires cloud delegation-key support and is covered by adapter tests rather than Azurite.
