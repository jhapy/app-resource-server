package org.jhapy.resource.service;

import org.jhapy.resource.domain.StoredFile;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-05-15
 */
public interface ResourceService {

  StoredFile save(StoredFile storedFile);

  StoredFile getById(String id);

  void delete(String id);
}
