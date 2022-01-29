package org.jhapy.resource.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.bson.types.ObjectId;
import org.jhapy.commons.config.AppProperties;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.cqrs.event.resource.StoredFileCreatedEvent;
import org.jhapy.cqrs.event.resource.StoredFileDeletedEvent;
import org.jhapy.cqrs.event.resource.StoredFileUpdatedEvent;
import org.jhapy.cqrs.query.resource.CountAnyMatchingStoredFileQuery;
import org.jhapy.cqrs.query.resource.GetStoredFileByIdQuery;
import org.jhapy.dto.serviceQuery.CountChangeResult;
import org.jhapy.resource.converter.StoredFileConverter;
import org.jhapy.resource.domain.PdfConvertEnum;
import org.jhapy.resource.domain.StoredFile;
import org.jhapy.resource.exception.EntityNotFoundException;
import org.jhapy.resource.repository.StoredFileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@ProcessingGroup("stored-file-group")
public class StoredFileEventHandler implements HasLogger {
  private final AppProperties appProperties;

  private final StoredFileRepository repository;
  private final StoredFileConverter converter;
  private final QueryUpdateEmitter queryUpdateEmitter;

  private final GridFsOperations operations;

  @ExceptionHandler
  public void handleException(Exception ex) throws Exception {
    String loggerPrefix = getLoggerPrefix("handleException");
    error(
        loggerPrefix,
        ex,
        "Exception in EventHandler (ExceptionHandler): {0}:{1}",
        ex.getClass().getName(),
        ex.getMessage());
    throw ex;
  }

  @EventHandler
  public void on(StoredFileCreatedEvent event) throws Exception {
    String loggerPrefix = getLoggerPrefix("onStoredFileCreatedEvent");
    debug(loggerPrefix, "In with : " + event.getId() + ", " + event.getFilename());

    StoredFile entity = converter.toEntity(event);
    entity = save(entity);
    queryUpdateEmitter.emit(
        GetStoredFileByIdQuery.class, query -> true, converter.asDTO(entity, null));

    queryUpdateEmitter.emit(
        CountAnyMatchingStoredFileQuery.class, query -> true, new CountChangeResult());

    debug(loggerPrefix, "Out with : " + event.getId() + ", " + event.getFilename());
  }

  @EventHandler
  public void on(StoredFileUpdatedEvent event) throws Exception {
    String loggerPrefix = getLoggerPrefix("onStoredFileUpdatedEvent");
    debug(loggerPrefix, "In with : " + event.getId() + ", " + event.getFilename());

    StoredFile entity = converter.toEntity(event);
    entity = save(entity);
    queryUpdateEmitter.emit(
        GetStoredFileByIdQuery.class, query -> true, converter.asDTO(entity, null));

    debug(loggerPrefix, "Out with : " + event.getId() + ", " + event.getFilename());
  }

  public StoredFile save(StoredFile entity) {
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    if (entity.getContent() != null && entity.getContent().length > 0) {
      if (entity.getId() == null) {
        entity.setPdfConvertStatus(PdfConvertEnum.NOT_CONVERTED);
      }
      if (entity.getMd5Content() == null) {
        entity.setMd5Content(DigestUtils.md5Digest(entity.getContent()));
      } else {
        byte[] md5 = DigestUtils.md5Digest(entity.getContent());
        boolean contentChanged = Arrays.equals(entity.getMd5Content(), md5);
        if (contentChanged) {
          entity.setPdfConvertStatus(PdfConvertEnum.NOT_CONVERTED);
          entity.setPdfContent(null);
          entity.setMd5Content(md5);
        }
      }
    } else {
      entity.setPdfConvertStatus(PdfConvertEnum.NOT_NEEDED);
      entity.setPdfContent(null);
      entity.setMd5Content(null);
    }

    byte[] content = entity.getContent();
    byte[] originalContent = entity.getOrginalContent();
    byte[] pdfContent = entity.getPdfContent();

    entity.setContent(null);
    entity.setOrginalContent(null);
    entity.setPdfContent(null);

    StoredFile savedEntity = repository.save(entity);

    DBObject fileMetaData = new BasicDBObject();
    fileMetaData.put("relatedObjectId", savedEntity.getId());

    if (content != null) {
      ObjectId objectId =
          operations.store(
              new ByteArrayInputStream(content),
              savedEntity.getId() + "-" + entity.getFilename(),
              entity.getMimeType(),
              fileMetaData);
      entity.setContentFileId(objectId.toString());
    }
    if (originalContent != null) {
      ObjectId objectId =
          operations.store(
              new ByteArrayInputStream(originalContent),
              savedEntity.getId() + "-o-" + entity.getFilename(),
              entity.getMimeType(),
              fileMetaData);
      entity.setOriginalContentFileId(objectId.toString());
    }
    if (pdfContent != null) {
      ObjectId objectId =
          operations.store(
              new ByteArrayInputStream(pdfContent),
              savedEntity.getId() + "-" + replaceExtension(entity.getFilename(), "pdf"),
              "application/pdf",
              fileMetaData);
      entity.setPdfContentFileId(objectId.toString());
    }

    savedEntity = repository.save(entity);

    return savedEntity;
  }

  @EventHandler
  public void on(StoredFileDeletedEvent event) throws Exception {
    StoredFile entity = repository.findById(event.getId()).orElse(null);
    if (entity == null) {
      throw new EntityNotFoundException();
    }

    if (entity.getContentFileId() != null) {
      operations.delete(new Query(Criteria.where("_id").is(entity.getContentFileId())));
    }
    if (entity.getOriginalContentFileId() != null) {
      operations.delete(new Query(Criteria.where("_id").is(entity.getOriginalContentFileId())));
    }
    if (entity.getPdfContentFileId() != null) {
      operations.delete(new Query(Criteria.where("_id").is(entity.getPdfContentFileId())));
    }

    repository.deleteById(event.getId());
  }

  // Every minutes
  @Scheduled(fixedRate = 60000)
  @Transactional
  protected void convertPdfs() {
    var loggerPrefix = getLoggerPrefix("convertPdfs");
    long start = System.currentTimeMillis();
    debug(loggerPrefix, "Starting to convert");
    Page<StoredFile> storedFiles =
        repository.findByPdfConvertStatus(PdfConvertEnum.NOT_CONVERTED, PageRequest.of(0, 100));
    debug(
        loggerPrefix,
        storedFiles.getTotalElements()
            + " elements not converted, will convert "
            + storedFiles.getContent().size()
            + " documents");
    AtomicInteger nbConverted = new AtomicInteger();
    AtomicInteger nbNotSupported = new AtomicInteger();
    storedFiles
        .getContent()
        .forEach(
            storedFile -> {
              String loggerPrefixLoop =
                  getLoggerPrefix("convertPdfs.loop", storedFile.getId(), storedFile.getFilename());
              if (storedFile.getMimeType().contains("pdf")
                  || storedFile.getMimeType().startsWith("image")) {
                storedFile.setPdfConvertStatus(PdfConvertEnum.NOT_NEEDED);
              } else {
                GridFSFile file =
                    operations.findOne(
                        new Query(Criteria.where("_id").is(storedFile.getContentFileId())));
                if (file != null) {
                  try {
                    storedFile.setContent(operations.getResource(file).getContent().readAllBytes());
                  } catch (IOException e) {
                    error(loggerPrefixLoop, "Cannot get file content : " + e.getMessage());
                  }
                } else {
                  error(loggerPrefixLoop, "GridFS file not found");
                }
                byte[] converted = convertToPdf(storedFile);
                if (converted == null) {
                  storedFile.setPdfConvertStatus(PdfConvertEnum.NOT_SUPPORTED);
                  nbNotSupported.getAndIncrement();
                } else {
                  // storedFile.setPdfContent(converted);
                  storedFile.setPdfConvertStatus(PdfConvertEnum.CONVERTED);
                  DBObject fileMetaData = new BasicDBObject();
                  fileMetaData.put("relatedObjectId", storedFile.getId());
                  ObjectId objectId =
                      operations.store(
                          new ByteArrayInputStream(converted),
                          storedFile.getId()
                              + "-"
                              + replaceExtension(storedFile.getFilename(), "pdf"),
                          "application/pdf",
                          fileMetaData);
                  storedFile.setPdfContentFileId(objectId.toString());

                  nbConverted.getAndIncrement();
                }
              }
              if (storedFile.getMd5Content() == null && storedFile.getContent() != null) {
                storedFile.setMd5Content(DigestUtils.md5Digest(storedFile.getContent()));
              }
            });
    debug(
        loggerPrefix,
        nbConverted.get() + " documents converted, " + nbNotSupported + " documents not supported");
    debug(
        loggerPrefix,
        "Save documents converted in " + (System.currentTimeMillis() - start) + " ms");
    repository.saveAll(storedFiles);
    debug(loggerPrefix, "Saved, total duration = " + (System.currentTimeMillis() - start) + " ms");
  }

  private String replaceExtension(String filename, String newExt) {
    int i = filename.lastIndexOf('.');
    if (i == -1) {
      return filename + "." + newExt;
    } else {
      return filename.substring(0, i) + "." + newExt;
    }
  }

  private byte[] convertToPdf(StoredFile entity) {
    var loggerPrefix = getLoggerPrefix("convertToPdf", entity.getId(), entity.getFilename());
    if (entity.getContent() == null || entity.getContent().length == 0) {
      warn(loggerPrefix, "Empty content, skip");
      return null;
    }
    byte[] fileContent = entity.getContent();
    if (!entity.getMimeType().contains("pdf") && !entity.getMimeType().startsWith("image")) {
      String ext = entity.getFilename().substring(entity.getFilename().lastIndexOf(".") + 1);

      File initialFile = null;
      Path tmpDir = null;
      Path resultFile = null;
      try {
        initialFile = File.createTempFile(entity.getId() + "-", "." + ext);

        tmpDir = Files.createTempDirectory(null);
        Files.write(initialFile.toPath(), fileContent);

        trace(
            loggerPrefix,
            "Exec : "
                + appProperties.getLibreoffice().getPath()
                + " --convert-to pdf --outdir "
                + tmpDir.toString()
                + " "
                + initialFile.getPath());

        List<String> commands = new ArrayList();
        commands.add(appProperties.getLibreoffice().getPath());
        commands.add("--convert-to");
        commands.add("pdf");
        commands.add("--outdir");
        commands.add(tmpDir.toString());
        commands.add(initialFile.getPath());
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commands);

        Process process = builder.start();
        BufferedReader stdInput =
            new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError =
            new BufferedReader(new InputStreamReader(process.getErrorStream()));
        int exitCode = process.waitFor();
        if (exitCode == 0) {
          String targetFile =
              initialFile.getName().substring(0, initialFile.getName().lastIndexOf("."));

          resultFile = Path.of(tmpDir.toString(), targetFile + ".pdf");
          fileContent = Files.readAllBytes(resultFile);
          debug(loggerPrefix, "Converted : " + entity.getMimeType());
        } else {
          error(loggerPrefix, "Cannot convert, exit code = " + exitCode);

          String s;
          StringBuilder strBuilder = new StringBuilder();
          while ((s = stdInput.readLine()) != null) {
            strBuilder.append(s).append("\n");
          }
          if (strBuilder.length() > 0) {
            error(loggerPrefix, "StdInput : " + strBuilder.substring(0, strBuilder.length() - 2));
          }

          strBuilder = new StringBuilder();
          while ((s = stdError.readLine()) != null) {
            strBuilder.append(s).append("\n");
          }
          if (strBuilder.length() > 0) {
            error(loggerPrefix, "StdError : " + strBuilder.substring(0, strBuilder.length() - 2));
          }

          fileContent = null;
        }
      } catch (IOException | InterruptedException e) {
        error(loggerPrefix, "Cannot convert, exception = " + e.getMessage());
        fileContent = null;
      } finally {
        if (initialFile != null) {
          initialFile.delete();
        }
        if (resultFile != null) {
          try {
            Files.delete(resultFile);
          } catch (IOException ignored) {
          }
        }
        if (tmpDir != null) {
          try {
            Files.delete(tmpDir);
          } catch (IOException ignored) {
          }
        }
      }
    } else {
      debug(loggerPrefix, "Image or PDF content, skip (" + entity.getMimeType() + ")");
      fileContent = null;
    }
    return fileContent;
  }
}
