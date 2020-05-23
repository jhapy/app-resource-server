package org.jhapy.resource.config.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@ChangeLog
public class InitialValuesChangeLog {

  @ChangeSet(order = "001", id = "createCollection", author = "jHapy Dev1")
  public void createCollection(MongoTemplate mongoTemplate) {
    mongoTemplate.createCollection("storedFile");
  }
}
