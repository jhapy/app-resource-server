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

package org.jhapy.resource.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-03-06
 */
@Data
@EqualsAndHashCode(exclude = {"createdBy", "modifiedBy", "created", "modified", "version"})
public abstract class BaseEntity implements Serializable {

  /** DB Generated ID */
  @Id private UUID id;

  /** Who create this record (no ID, use username) */
  @CreatedBy private String createdBy;

  /** When this record has been created */
  @CreatedDate private Instant created;

  /** How did the last modification of this record (no ID, use username) */
  @LastModifiedBy private String modifiedBy;

  /** When this record was last updated */
  @LastModifiedDate private Instant modified;

  /** Version of the record. Used for synchronization and concurrent access. */
  @Version private Long version;

  /** Indicate if the current record is active (deactivate instead of delete) */
  private Boolean isActive = Boolean.TRUE;
}