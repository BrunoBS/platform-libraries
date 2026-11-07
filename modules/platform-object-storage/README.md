# Platform Object Storage

Provider-neutral AWS S3 and Azure Blob Storage capability.

The public contract, configuration, errors, examples, and provider limitations are documented in the [Golden Platform module guide](https://github.com/BrunoBS/golden-platform/blob/main/docs/libraries/modules/platform-object-storage.md).

The consumer API is under `br.com.portalmanager.platform.library.objectstorage.api` and is grouped by responsibility:

- `configuration`: Spring configuration and provider properties.
- `contract`: provider-neutral client interfaces.
- `contract.request`: inputs accepted by the clients.
- `contract.response`: results returned by the clients.
- `contract.model`: value types shared across requests and responses.

Provider adapters and implementation helpers are internal to the module; earlier package paths are not maintained as aliases.

- [POM and dependencies](pom.xml)
- [Reactor development and verification](../../README.md)

Integration tests require Docker and use LocalStack for AWS and Azurite for Azure-compatible storage operations. Azure user delegation SAS generation requires cloud delegation-key support and is covered by adapter tests rather than Azurite.
