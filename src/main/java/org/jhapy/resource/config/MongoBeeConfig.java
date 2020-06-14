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

package org.jhapy.resource.config;

import com.github.cloudyrock.mongock.driver.mongodb.springdata.v2.SpringDataMongo2Driver;
import com.github.cloudyrock.spring.v5.MongockSpring5;
import org.jhapy.commons.utils.HasLogger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@DependsOn("mongoTemplate")
public class MongoBeeConfig implements HasLogger {

  private static final String MONGODB_CHANGELOGS_PACKAGE = "org.jhapy.resource.config.changelogs";

  @Bean
  public InitializingBean mongock(MongoTemplate mongoTemplate,
      ApplicationContext applicationContext) {
    String loggerPrefix = getLoggerPrefix("mongobeeGlobal");

    SpringDataMongo2Driver driver = new SpringDataMongo2Driver(mongoTemplate);
    driver.setChangeLogCollectionName("dbChangelog"); // compatibility with mongobee
    driver.setLockCollectionName("dbChangelogLock");

    return MongockSpring5.builder().setDriver(driver)
        .addChangeLogsScanPackage(MONGODB_CHANGELOGS_PACKAGE).setSpringContext(applicationContext)
        .buildInitializingBeanRunner();
  }
}
