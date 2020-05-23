package org.jhapy.resource.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.resource.domain.StoredFile;
import org.jhapy.resource.exception.EntityNotFoundException;
import org.jhapy.resource.repository.StoredFileRepository;

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
