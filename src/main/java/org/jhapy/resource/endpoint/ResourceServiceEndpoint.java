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

package org.jhapy.resource.endpoint;

import org.jhapy.commons.endpoint.BaseEndpoint;
import org.jhapy.dto.serviceQuery.ServiceResult;
import org.jhapy.dto.serviceQuery.generic.DeleteByStrIdQuery;
import org.jhapy.dto.serviceQuery.generic.GetByStrIdQuery;
import org.jhapy.dto.serviceQuery.generic.SaveQuery;
import org.jhapy.resource.converter.ResourceConverterV2;
import org.jhapy.resource.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-06-05
 */

@RestController
@RequestMapping("/api/resourceService")
public class ResourceServiceEndpoint extends BaseEndpoint {

  private final ResourceService resourceService;

  public ResourceServiceEndpoint(ResourceService resourceService,
      ResourceConverterV2 converter) {
    super(converter);
    this.resourceService = resourceService;
  }

  protected ResourceConverterV2 getConverter() {
    return (ResourceConverterV2) converter;
  }

  @PostMapping(value = "/getById")
  public ResponseEntity<ServiceResult> getById(@RequestBody GetByStrIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getById");
    return handleResult(loggerPrefix,
        getConverter().convertToDto(resourceService.getById(query.getId())));
  }

  @PostMapping(value = "/getByIdNoContent")
  public ResponseEntity<ServiceResult> getByIdNoContent(@RequestBody GetByStrIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getByIdNoContent");
    return handleResult(loggerPrefix,
        getConverter().convertToDto(resourceService.getByIdNoContent(query.getId())));
  }

  @PostMapping(value = "/getByIdPdfContent")
  public ResponseEntity<ServiceResult> getByIdPdfContent(@RequestBody GetByStrIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getByIdPdfContent");
    return handleResult(loggerPrefix,
        getConverter().convertToDto(resourceService.getByIdPdfContent(query.getId())));
  }

  @PostMapping(value = "/save")
  public ResponseEntity<ServiceResult> save(
      @RequestBody SaveQuery<org.jhapy.dto.utils.StoredFile> query) {
    var loggerPrefix = getLoggerPrefix("save");
    return handleResult(loggerPrefix, getConverter()
        .convertToDto(resourceService.save(getConverter().convertToDomain(query.getEntity()))));
  }

  @PostMapping(value = "/delete")
  public ResponseEntity<ServiceResult> delete(@RequestBody DeleteByStrIdQuery query) {
    var loggerPrefix = getLoggerPrefix("delete");
    resourceService.delete(query.getId());
    return handleResult(loggerPrefix);
  }
}