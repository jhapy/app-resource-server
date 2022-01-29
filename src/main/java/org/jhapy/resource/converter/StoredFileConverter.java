package org.jhapy.resource.converter;

import org.jhapy.cqrs.event.resource.StoredFileCreatedEvent;
import org.jhapy.cqrs.event.resource.StoredFileUpdatedEvent;
import org.jhapy.dto.domain.resource.StoredFileDTO;
import org.jhapy.resource.command.StoredFileAggregate;
import org.jhapy.resource.domain.StoredFile;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.Map;

@Mapper(config = BaseNosqlDbConverterConfig.class, componentModel = "spring")
public abstract class StoredFileConverter extends GenericMapper<StoredFile, StoredFileDTO> {
  public static StoredFileConverter INSTANCE = Mappers.getMapper(StoredFileConverter.class);

  public abstract StoredFileCreatedEvent toStoredFileCreatedEvent(StoredFileDTO dto);

  public abstract StoredFileUpdatedEvent toStoredFileUpdatedEvent(StoredFileDTO dto);

  public abstract void updateAggregateFromStoredFileCreatedEvent(
      StoredFileCreatedEvent event, @MappingTarget StoredFileAggregate aggregate);

  public abstract void updateAggregateFromStoredFileUpdatedEvent(
      StoredFileUpdatedEvent event, @MappingTarget StoredFileAggregate aggregate);

  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modified", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  public abstract StoredFile toEntity(StoredFileCreatedEvent event);

  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modified", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  public abstract StoredFile toEntity(StoredFileUpdatedEvent event);

  public abstract StoredFile asEntity(StoredFileDTO dto, @Context Map<String, Object> context);

  public abstract StoredFileDTO asDTO(StoredFile domain, @Context Map<String, Object> context);

  @AfterMapping
  protected void afterConvert(
      StoredFileDTO dto, @MappingTarget StoredFile domain, @Context Map<String, Object> context) {}

  @AfterMapping
  protected void afterConvert(
      StoredFile domain, @MappingTarget StoredFileDTO dto, @Context Map<String, Object> context) {}
}
