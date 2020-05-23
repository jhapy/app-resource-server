package org.jhapy.resource.service;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.jhapy.resource.domain.BaseEntity;
import org.jhapy.resource.exception.EntityNotFoundException;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-03-26
 */
@Transactional(readOnly = true)
public interface CrudService<T extends BaseEntity> {

  MongoRepository<T, Long> getRepository();

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
  default void delete(long id) {
    delete(load(id));
  }

  default long count() {
    return getRepository().count();
  }

  default T load(long id) {
    T entity = getRepository().findById(id).orElse(null);
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    return entity;
  }

  default Iterable<T> findAll() {
    return getRepository().findAll();
  }
}
