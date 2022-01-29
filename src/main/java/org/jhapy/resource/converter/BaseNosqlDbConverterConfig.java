package org.jhapy.resource.converter;

import org.jhapy.cqrs.command.AbstractBaseAggregate;
import org.jhapy.cqrs.event.AbstractBaseEvent;
import org.jhapy.dto.domain.BaseEntity;
import org.mapstruct.*;

import java.util.Map;

@MapperConfig(
    mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG,
    builder = @Builder(disableBuilder = true))
public interface BaseNosqlDbConverterConfig {

  @Mapping(target = "clientName", ignore = true)
  BaseEntity asDTO(
      org.jhapy.resource.domain.BaseEntity domain, @Context Map<String, Object> context);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modified", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  org.jhapy.resource.domain.BaseEntity asEntity(
      BaseEntity domain, @Context Map<String, Object> context);

  @Mapping(target = "converter", ignore = true)
  AbstractBaseAggregate toAggregate(AbstractBaseEvent event);
}
