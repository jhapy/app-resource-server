package org.jhapy.resource.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.jhapy.commons.utils.OrikaBeanMapper;
import org.jhapy.dto.utils.StoredFile;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-06-05
 */
@Component
public class ResourcesConverter {

  private final OrikaBeanMapper orikaBeanMapper;

  public ResourcesConverter(OrikaBeanMapper orikaBeanMapper) {
    this.orikaBeanMapper = orikaBeanMapper;
  }

  @Bean
  public void resourcesConverters() {
    orikaBeanMapper.addMapper(StoredFile.class,
        org.jhapy.resource.domain.StoredFile.class);
    orikaBeanMapper.addMapper(org.jhapy.resource.domain.StoredFile.class, StoredFile.class);
  }
}