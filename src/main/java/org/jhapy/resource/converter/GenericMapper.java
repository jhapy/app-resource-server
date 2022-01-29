package org.jhapy.resource.converter;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Context;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class GenericMapper<E, D> extends BaseConverterV2 {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modified", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  public abstract E asEntity(D dto, @Context Map<String, Object> context);

  @Mapping(target = "isNew", ignore = true)
  @Mapping(target = "clientName", ignore = true)
  public abstract D asDTO(E entity, @Context Map<String, Object> context);

  public abstract List<E> asEntityList(Iterable<D> dtoList, @Context Map<String, Object> context);

  public abstract List<D> asDTOList(Iterable<E> entityList, @Context Map<String, Object> context);

  public List<String> map(String value) {
    if (StringUtils.isNotBlank(value)) return Arrays.asList(value.split(","));
    else return null;
  }

  public String map(List<String> values) {
    if (values != null && !values.isEmpty()) return String.join(",", values);
    else return null;
  }
}
