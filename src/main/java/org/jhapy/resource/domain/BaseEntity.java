package org.jhapy.resource.domain;

import java.io.Serializable;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-03-06
 */
@Data
@EqualsAndHashCode(exclude = {"createdBy", "modifiedBy", "created", "modified", "version"})
public abstract class BaseEntity implements Serializable {

  /**
   * DB Generated ID
   */
  @Id
  private String id;

  /**
   * Who create this record (no ID, use username)
   */
  @CreatedBy
  private String createdBy;

  /**
   * When this record has been created
   */
  @CreatedDate
  private Instant created;

  /**
   * How did the last modification of this record (no ID, use username)
   */
  @LastModifiedBy
  private String modifiedBy;

  /**
   * When this record was last updated
   */
  @LastModifiedDate
  private Instant modified;

  /**
   * Version of the record. Used for synchronization and concurrent access.
   */
  @Version
  private Long version;

  /**
   * Indicate if the current record is active (deactivate instead of delete)
   */
  private Boolean isActive = Boolean.TRUE;
}
