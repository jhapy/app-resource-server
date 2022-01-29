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

import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.jhapy.cqrs.query.resource.*;
import org.jhapy.dto.domain.resource.StoredFileDTO;
import org.jhapy.dto.serviceQuery.ServiceResult;
import org.jhapy.dto.serviceQuery.generic.GetByIdQuery;
import org.jhapy.resource.domain.StoredFile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-06-05
 */
@RestController
@RequestMapping("/api/resourceService")
public class ResourceServiceEndpoint extends BaseEndpoint<StoredFile, StoredFileDTO> {

  public ResourceServiceEndpoint(CommandGateway commandGateway, QueryGateway queryGateway) {
    super(commandGateway, queryGateway);
  }

  @PostMapping(value = "/getByIdNoContent")
  public ResponseEntity<ServiceResult> getByIdNoContent(@RequestBody GetByIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getByIdNoContent");

    return handleResult(
        loggerPrefix,
        queryGateway
            .query(
                new GetStoredFileByIdNoContentQuery(query.getId()),
                ResponseTypes.instanceOf(GetStoredFileByIdNoContentResponse.class))
            .join());
  }

  @PostMapping(value = "/getByIdPdfContent")
  public ResponseEntity<ServiceResult> getByIdPdfContent(@RequestBody GetByIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getByIdPdfContent");
    return handleResult(
        loggerPrefix,
        queryGateway
            .query(
                new GetStoredFileByIdPdfContentQuery(query.getId()),
                ResponseTypes.instanceOf(GetStoredFileByIdPdfContentResponse.class))
            .join());
  }

  @GetMapping(value = "/download")
  public ResponseEntity<byte[]> download(@RequestBody GetByIdQuery query) {
    GetStoredFileByIdResponse storedFileByIdResponse =
        queryGateway
            .query(
                new GetStoredFileByIdQuery(query.getId()),
                ResponseTypes.instanceOf(GetStoredFileByIdResponse.class))
            .join();

    var headers = new HttpHeaders();
    var storedFile = storedFileByIdResponse.getData();
    headers.setCacheControl(CacheControl.noCache().getHeaderValue());
    headers.setPragma("no-cache");
    headers.setExpires(0);
    if (StringUtils.isNotBlank(storedFile.getMimeType()))
      headers.setContentType(MediaType.valueOf(storedFile.getMimeType()));
    if (StringUtils.isNotBlank(storedFile.getFilename()))
      headers.setContentDisposition(
          ContentDisposition.attachment()
              .filename(storedFile.getFilename().replace(" ", "_"))
              .build());

    return new ResponseEntity<>(storedFile.getContent(), headers, HttpStatus.OK);
  }
}
