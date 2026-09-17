package com.empresa.platform.crud.dto;

import com.empresa.platform.crud.version.VersionedResource;

public interface VersionedCrudDTO<ID> extends BaseCrudDTO<ID>, VersionedResource {
}
