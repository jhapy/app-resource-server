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

package org.jhapy.resource.config.changelogs;

import io.mongock.api.annotations.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.resource.domain.StoredFile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@ChangeUnit(order = "001", id = "initialValuesChangeLog", author = "jHapy Dev1")
public class InitialValuesChangeLog implements HasLogger {

  @BeforeExecution
  public void beforeExecution(MongoTemplate mongoTemplate) {

    mongoTemplate.createCollection(StoredFile.class);
  }

  @RollbackBeforeExecution
  public void rollbackBeforeExecution(MongoTemplate mongoTemplate) {

    mongoTemplate.dropCollection(StoredFile.class);
  }

  @Execution
  public void execution(CommandGateway commandGateway) {}

  @RollbackExecution
  public void rollbackExecution(CommandGateway commandGateway) {}
}
