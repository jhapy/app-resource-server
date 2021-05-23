package org.jhapy.resource.converter;

import java.util.Collection;
import java.util.List;
import org.jhapy.commons.converter.CommonsConverterV2;
import org.jhapy.dto.domain.audit.Session;
import org.jhapy.dto.utils.StoredFile;
import org.mapstruct.Mapper;

/**
 * @author Alexandre Clavaud.
 * @version 1.0
 * @since 18/05/2021
 */
@Mapper(componentModel = "spring")
public abstract class ResourceConverterV2 extends CommonsConverterV2 {

  public abstract StoredFile convertToDto(org.jhapy.resource.domain.StoredFile domain);

  public abstract org.jhapy.resource.domain.StoredFile convertToDomain(StoredFile dto);

  public abstract List<Session> convertToDtoStoredFiles(
      Collection<org.jhapy.resource.domain.StoredFile> domains);

  public abstract List<org.jhapy.resource.domain.StoredFile> convertToDomainStoredFiles(
      Collection<StoredFile> dtos);
}
