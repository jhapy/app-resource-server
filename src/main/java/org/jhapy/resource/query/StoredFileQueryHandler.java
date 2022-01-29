package org.jhapy.resource.query;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.queryhandling.QueryHandler;
import org.jhapy.cqrs.query.resource.*;
import org.jhapy.dto.domain.resource.StoredFileDTO;
import org.jhapy.resource.converter.GenericMapper;
import org.jhapy.resource.converter.StoredFileConverter;
import org.jhapy.resource.domain.StoredFile;
import org.jhapy.resource.repository.StoredFileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
@RequiredArgsConstructor
public class StoredFileQueryHandler implements BaseQueryHandler<StoredFile, StoredFileDTO> {
  private final StoredFileRepository repository;
  private final StoredFileConverter converter;
  private final MongoTemplate mongoTemplate;

  private final GridFsOperations operations;

  @QueryHandler
  public GetStoredFileByIdResponse getById(GetStoredFileByIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getById", query.getId());

    StoredFile storedFile = repository.findById(query.getId()).orElse(null);
    if (storedFile != null) {
      loggerPrefix = getLoggerPrefix("getById", query.getId(), storedFile.getFilename());
      if (storedFile.getContentFileId() != null) {
        logger().debug(loggerPrefix + "Get Content file");
        GridFSFile file =
            operations.findOne(new Query(Criteria.where("_id").is(storedFile.getContentFileId())));
        if (file != null) {
          try {
            storedFile.setContent(operations.getResource(file).getContent().readAllBytes());
          } catch (IOException e) {
            logger().error(loggerPrefix + "Cannot get file content : " + e.getMessage());
          }
        } else {
          logger().error(loggerPrefix + "GridFS file not found : " + storedFile.getFilename());
        }
      }
      if (storedFile.getOriginalContentFileId() != null) {
        logger().debug(loggerPrefix + "Get Original Content file");
        GridFSFile file =
            operations.findOne(
                new Query(Criteria.where("_id").is(storedFile.getOriginalContentFileId())));
        if (file != null) {
          try {
            storedFile.setOrginalContent(operations.getResource(file).getContent().readAllBytes());
          } catch (IOException e) {
            logger().error(loggerPrefix + "Cannot get original file content : " + e.getMessage());
          }
        } else {
          logger().error(loggerPrefix + "GridFS original file not found");
        }
      }
      if (storedFile.getPdfContentFileId() != null) {
        logger().debug(loggerPrefix + "Get Pdf Content file");
        GridFSFile file =
            operations.findOne(
                new Query(Criteria.where("_id").is(storedFile.getPdfContentFileId())));
        if (file != null) {
          try {
            storedFile.setPdfContent(operations.getResource(file).getContent().readAllBytes());
          } catch (IOException e) {
            logger().error(loggerPrefix + "Cannot get pdf file content : " + e.getMessage());
          }
        } else {
          logger().error(loggerPrefix + "GridFS pdf file not found");
        }
      }
    } else {
      logger().debug(loggerPrefix + "File not found");
    }

    return new GetStoredFileByIdResponse(converter.asDTO(storedFile, null));
  }

  @QueryHandler
  public GetStoredFileByIdNoContentResponse getByIdNoContent(
      GetStoredFileByIdNoContentQuery query) {
    StoredFile storedFile = repository.findById(query.getId()).orElse(null);
    if (storedFile != null) {
      storedFile.setContent(null);
      storedFile.setPdfContent(null);
      storedFile.setOrginalContent(null);
      return new GetStoredFileByIdNoContentResponse(converter.asDTO(storedFile, null));
    } else {
      return null;
    }
  }

  @QueryHandler
  public GetStoredFileByIdPdfContentResponse getByIdPdfContent(
      GetStoredFileByIdPdfContentQuery query) {
    var loggerPrefix = getLoggerPrefix("getByIdPdfContent", query.getId());

    StoredFile storedFile = repository.findById(query.getId()).orElse(null);
    if (storedFile != null) {
      storedFile.setContent(null);
      storedFile.setOrginalContent(null);

      if (storedFile.getPdfContentFileId() != null) {
        GridFSFile file =
            operations.findOne(
                new Query(Criteria.where("_id").is(storedFile.getPdfContentFileId())));
        if (file != null) {
          try {
            storedFile.setPdfContent(operations.getResource(file).getContent().readAllBytes());
          } catch (IOException e) {
            logger().error(loggerPrefix + "Cannot get pdf file content : " + e.getMessage());
          }
        } else {
          logger().error(loggerPrefix + "GridFS pdf file not found");
        }
      }

      return new GetStoredFileByIdPdfContentResponse(converter.asDTO(storedFile, null));
    } else {
      return null;
    }
  }

  @QueryHandler
  public GetAllStoredFilesResponse getAll(GetAllStoredFilesQuery query) {
    return new GetAllStoredFilesResponse(converter.asDTOList(repository.findAll(), null));
  }

  @QueryHandler
  public FindAnyMatchingStoredFileResponse findAnyMatchingStoredFile(
      FindAnyMatchingStoredFileQuery query) {
    Page<StoredFile> result =
        BaseQueryHandler.super.findAnyMatching(
            query.getFilter(), query.getShowInactive(), converter.convert(query.getPageable()));
    return new FindAnyMatchingStoredFileResponse(converter.asDTOList(result.getContent(), null));
  }

  @QueryHandler
  public CountAnyMatchingStoredFileResponse countAnyMatchingStoredFile(
      CountAnyMatchingStoredFileQuery query) {
    return new CountAnyMatchingStoredFileResponse(
        BaseQueryHandler.super.countAnyMatching(query.getFilter(), query.getShowInactive()));
  }

  public void buildSearchQuery(Criteria rootCriteria, String filter, Boolean showInactive) {
    String loggerPrefix = getLoggerPrefix("buildSearchQuery");
    List<Criteria> andPredicated = new ArrayList<>();

    if (StringUtils.isNoneBlank(filter)) {
      andPredicated.add(
          (new Criteria())
              .orOperator(where("filename").regex(filter), where("mimeType").regex(filter)));
    }

    if (showInactive == null || !showInactive) {
      andPredicated.add(Criteria.where("isActive").is(Boolean.TRUE));
    }

    if (!andPredicated.isEmpty()) rootCriteria.andOperator(andPredicated.toArray(new Criteria[0]));
  }

  @Override
  public MongoRepository<StoredFile, UUID> getRepository() {
    return repository;
  }

  @Override
  public MongoTemplate getMongoTemplate() {
    return mongoTemplate;
  }

  @Override
  public Class<StoredFile> getEntityClass() {
    return StoredFile.class;
  }

  @Override
  public GenericMapper<StoredFile, StoredFileDTO> getConverter() {
    return converter;
  }
}
