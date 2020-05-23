package org.jhapy.resource.config;

import com.github.mongobee.Mongobee;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.jhapy.commons.utils.HasLogger;

@Configuration
@DependsOn("mongoTemplate")
public class MongoBeeConfig implements HasLogger {

  private static final String MONGODB_CHANGELOGS_PACKAGE = "org.jhapy.resource.config.changelogs";

  @Bean
  public Mongobee mongobeeGlobal(MongoClient mongoClient, MongoTemplate mongoTemplate,
      org.springframework.boot.autoconfigure.mongo.MongoProperties mongoProperties) {
    String loggerPrefix = getLoggerPrefix("mongobeeGlobal");
    Mongobee mongobee = new Mongobee(mongoClient);
    mongobee.setChangelogCollectionName("dbChangelog");
    mongobee.setLockCollectionName("dbChangelogLock");
    MongoClientURI uri = new MongoClientURI(mongoProperties.getUri());
    logger().debug(loggerPrefix + "uri = " + mongoProperties.getUri());
    mongobee.setMongoClientURI(uri);
    logger().debug(loggerPrefix + "database = " + uri.getDatabase());
    mongobee.setDbName(uri.getDatabase());
    mongobee.setMongoTemplate(mongoTemplate);
    // package to scan for migrations
    mongobee.setChangeLogsScanPackage(MONGODB_CHANGELOGS_PACKAGE);
    mongobee.setEnabled(true);
    return mongobee;
  }
}
