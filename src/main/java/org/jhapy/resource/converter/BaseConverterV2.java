package org.jhapy.resource.converter;

import org.jhapy.commons.converter.CommonsConverterV2;
import org.mapstruct.Mapper;

/**
 * @author Alexandre Clavaud.
 * @version 1.0
 * @since 18/05/2021
 */
@Mapper(componentModel = "spring")
public abstract class BaseConverterV2 extends CommonsConverterV2 {}
