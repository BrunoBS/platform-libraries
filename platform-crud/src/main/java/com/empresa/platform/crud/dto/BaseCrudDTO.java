package com.empresa.platform.crud.dto;

public interface BaseCrudDTO<ID, D extends BaseCrudDTO<ID, D>> {

    ID id();

    D withId(ID id);
}
