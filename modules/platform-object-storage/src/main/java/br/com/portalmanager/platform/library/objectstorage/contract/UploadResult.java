package br.com.portalmanager.platform.library.objectstorage.contract;

public record UploadResult(ObjectReference reference, String eTag, long sizeBytes) { }
