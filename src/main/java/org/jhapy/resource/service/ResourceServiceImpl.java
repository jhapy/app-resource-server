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

import org.jhapy.commons.utils.HasLogger;
import org.jhapy.resource.domain.StoredFile;
import org.jhapy.resource.exception.EntityNotFoundException;
import org.jhapy.resource.repository.StoredFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-05-15
 */
@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService, HasLogger {

  private final StoredFileRepository storedFileRepository;

  public ResourceServiceImpl(
      StoredFileRepository storedFileRepository) {
    this.storedFileRepository = storedFileRepository;
  }

  @Override
  @Transactional
  public void delete(String id) {
    if (id == null) {
      throw new EntityNotFoundException();
    }
    StoredFile entity = storedFileRepository.findById(id).orElse(null);
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    storedFileRepository.delete(entity);
  }

  @Override
  public StoredFile getById(String id) {
    return storedFileRepository.findById(id).orElse(null);
  }

  @Override
  @Transactional
  public StoredFile save(StoredFile entity) {
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    return storedFileRepository.save(entity);
  }
}
