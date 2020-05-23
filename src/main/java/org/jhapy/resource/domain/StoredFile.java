package org.jhapy.resource.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-05-15
 */
@Document(collection = "storedFile")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"content", "orginalContent"})
public class StoredFile extends BaseEntity {

  private String filename;
  private String mimeType;
  private long filesize;

  private byte[] content;
  private byte[] orginalContent;

  private float zoom;

  private Long relatedObjectId;
  private String relatedObjectClass;
}