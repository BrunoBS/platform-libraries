# Platform Object Storage

Provider-neutral data-plane API for AWS S3 and Azure Blob Storage. Applications depend on `ObjectStorageClient` and use logical destinations; AWS and Azure SDK types stay inside this module.

## Configuration

```yaml
platform:
  object-storage:
    provider: AWS # AWS or AZURE
    region: us-east-1
    destinations:
      audit:
        bucket: golden-audit
        prefix: objects
        suffix: .json
```

For Azure, set `provider: AZURE`, `endpoint` to the storage account Blob endpoint, and configure `container` instead of `bucket` for each destination. Azure authentication uses `DefaultAzureCredential`; AWS uses the standard SDK credential chain. Destinations refer to buckets and containers provisioned by infrastructure. The module does not create or administer those resources.

## Public operations

`ObjectStorageClient` provides upload, streaming download, properties (`head`), existence, paginated object listing, paginated version listing, lookup by ETag, deletion of the current object, and deletion of one exact version. Inputs use a logical destination plus an object key. A destination may add a prefix and suffix to the physical key; callers continue to use the logical key. Keys reject leading slashes, backslashes, empty path segments, `.` and `..`, and repeated destination affixes.

`ObjectReference.versionId` is optional; when supplied, reads address that version. `UploadRequest` defaults to `CREATE_ONLY`, enforced atomically by provider request conditions; use `OVERWRITE` when replacement is intended. Upload accepts provider-neutral content headers (`contentType`, `cacheControl`, `contentDisposition`, `contentEncoding`, and `contentLanguage`) and custom metadata. The returned `UploadResult.sha256` is the Base64-encoded SHA-256 calculated while the known-length stream is consumed.

```java
UploadResult uploaded = storage.upload(
        new UploadRequest("audit", "feature.json", bytes.length, "application/json", metadata),
        new ByteArrayInputStream(bytes));

try (DownloadedObject object = storage.download(uploaded.reference())) {
    object.content().transferTo(outputStream);
}
```

The caller owns the upload input stream and closes it. The caller must close `DownloadedObject` to release the download stream. Object listing returns only fields available from provider listing responses, avoiding a properties request per object; call `head` when headers or custom metadata are needed.

Version IDs are opaque and may be absent when provider versioning is disabled. `findVersionByETag` pages through the key's versions and is intended for Azure Blob Created notifications, which carry an ETag but not a version ID. For versioned S3 notifications, pass the notification's version ID directly. ETags are provider validators and are not guaranteed to be content hashes.

Errors use the `platform-messaging` contracts: `ValidationException` for invalid requests, `NotFoundException`, `UnauthorizedException`, `ConflictException`, and `ForbiddenException` for provider responses, `ApiException` for timeout and operational failures, and `PlatformConfigurationException` for invalid startup configuration. Message keys are provided through the standard `META-INF/platform-messages` bundles; provider SDK exceptions are retained only as causes and do not escape the public API.

Temporary signed URLs (S3 presigned URLs and Azure SAS URLs) are not currently part of `ObjectStorageClient`. They should be added as a separate public contract with explicit operation, expiry, and required upload headers; treat the returned URL as a bearer credential.
