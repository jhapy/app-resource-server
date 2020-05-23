package org.jhapy.resource.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.jhapy.resource.domain.StoredFile;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-05-15
 */
public interface StoredFileRepository extends MongoRepository<StoredFile, String> {

}
