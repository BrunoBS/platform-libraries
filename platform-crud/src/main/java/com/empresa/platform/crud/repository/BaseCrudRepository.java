package com.empresa.platform.crud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseCrudRepository<E, ID> extends JpaRepository<E, ID> {
}
