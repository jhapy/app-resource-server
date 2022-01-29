/*
 * Copyright 2020-2020 the original author or authors from the JHapy project.
 *
 * This file is part of the JHapy project, see https://www.jhapy.org/ for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jhapy.resource.service;

import org.jhapy.dto.domain.exception.EntityNotFoundException;
import org.jhapy.resource.domain.BaseEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-03-26
 */
@Transactional(readOnly = true)
public interface BaseCrudService<T extends BaseEntity> {

  MongoRepository<T, UUID> getRepository();

  @Transactional
  default T save(T entity) {
    return getRepository().save(entity);
  }

  @Transactional
  default void delete(T entity) {
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    getRepository().delete(entity);
  }

  @Transactional
  default void delete(UUID id) {
    delete(load(id));
  }

  default long count() {
    return getRepository().count();
  }

  default T load(UUID id) {
    var entity = getRepository().findById(id).orElse(null);
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    return entity;
  }

  default Iterable<T> findAll() {
    return getRepository().findAll();
  }
}
