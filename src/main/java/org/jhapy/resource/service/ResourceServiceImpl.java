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

package org.jhapy.resource.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jhapy.commons.config.AppProperties;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.resource.domain.PdfConvert;
import org.jhapy.resource.domain.StoredFile;
import org.jhapy.resource.exception.EntityNotFoundException;
import org.jhapy.resource.repository.StoredFileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-05-15
 */
@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService, HasLogger {

  private final AppProperties appProperties;
  private final StoredFileRepository storedFileRepository;

  public ResourceServiceImpl(
      AppProperties appProperties,
      StoredFileRepository storedFileRepository) {
    this.appProperties = appProperties;
    this.storedFileRepository = storedFileRepository;
  }

  @Override
  @Transactional
  public void delete(String id) {
    if (id == null) {
      throw new EntityNotFoundException();
    }
    StoredFile entity = storedFileRepository.findById(id).orElse(null);
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    storedFileRepository.delete(entity);
  }

  @Override
  public StoredFile getById(String id) {
    return storedFileRepository.findById(id).orElse(null);
  }

  @Override
  public StoredFile getByIdNoContent(String id) {
    StoredFile storedFile = storedFileRepository.findById(id).orElse(null);
    if (storedFile != null) {
      storedFile.setContent(null);
      storedFile.setPdfContent(null);
      storedFile.setOrginalContent(null);
      return storedFile;
    } else {
      return null;
    }
  }

  @Override
  public StoredFile getByIdPdfContent(String id) {
    StoredFile storedFile = storedFileRepository.findById(id).orElse(null);
    if (storedFile != null) {
      storedFile.setContent(null);
      storedFile.setOrginalContent(null);
      return storedFile;
    } else {
      return null;
    }
  }

  // Every minutes
  @Scheduled(fixedRate = 60000)
  @Transactional
  protected void convertPdfs() {
    String loggerPrefix = getLoggerPrefix("convertPdfs");
    long start = System.currentTimeMillis();
    logger().debug(loggerPrefix + "Starting to convert");
    Page<StoredFile> storedFiles = storedFileRepository
        .findByPdfConvertStatus(PdfConvert.NOT_CONVERTED,
            PageRequest.of(0, 100));
    logger().debug(
        loggerPrefix + storedFiles.getTotalElements() + " elements not converted, will convert "
            + storedFiles.getContent().size() + " documents");
    AtomicInteger nbConverted = new AtomicInteger();
    AtomicInteger nbNotSupported = new AtomicInteger();
    storedFiles.getContent().forEach(storedFile -> {
      byte[] converted = convertToPdf(storedFile);
      if (converted == null) {
        storedFile.setPdfConvertStatus(PdfConvert.NOT_SUPPORTED);
        nbNotSupported.getAndIncrement();
      } else {
        storedFile.setPdfContent(converted);
        storedFile.setPdfConvertStatus(PdfConvert.CONVERTED);
        nbConverted.getAndIncrement();
      }
      if (storedFile.getMd5Content() == null) {
        storedFile.setMd5Content(DigestUtils.md5Digest(storedFile.getContent()));
      }
    });
    logger().debug(loggerPrefix + nbConverted.get() + " documents converted, " + nbNotSupported
        + " documentes not supported");
    logger().debug(
        loggerPrefix + "Save documents converted in " + (System.currentTimeMillis() - start)
            + " ms");
    storedFileRepository.saveAll(storedFiles);
    logger().debug(
        loggerPrefix + "Saved, total duration = " + (System.currentTimeMillis() - start) + " ms");
  }

  @Override
  @Transactional
  public StoredFile save(StoredFile entity) {
    if (entity == null) {
      throw new EntityNotFoundException();
    }
    if (entity.getContent() != null && entity.getContent().length > 0) {
      if (entity.getId() == null) {
        entity.setPdfConvertStatus(PdfConvert.NOT_CONVERTED);
      }
      if (entity.getMd5Content() == null) {
        entity.setMd5Content(DigestUtils.md5Digest(entity.getContent()));
      } else {
        byte[] md5 = DigestUtils.md5Digest(entity.getContent());
        boolean contentChanged = Arrays.equals(entity.getMd5Content(), md5);
        if (contentChanged) {
          entity.setPdfConvertStatus(PdfConvert.NOT_CONVERTED);
          entity.setPdfContent(null);
          entity.setMd5Content(md5);
        }
      }
    } else {
      entity.setPdfConvertStatus(PdfConvert.NOT_SUPPORTED);
      entity.setPdfContent(null);
      entity.setMd5Content(null);
    }

    return storedFileRepository.save(entity);
  }

  private byte[] convertToPdf(StoredFile entity) {
    String loggerPrefix = getLoggerPrefix("convertToPdf", entity.getId(), entity.getFilename());
    if (entity.getContent() == null || entity.getContent().length == 0) {
      logger().warn(loggerPrefix + "Empty content, skip");
      return null;
    }
    byte[] fileContent = entity.getContent();
    String filename = entity.getFilename();
    if (!entity.getMimeType().contains("pdf") && !entity.getMimeType().startsWith("image")) {
      String filenameNoExt = entity.getFilename()
          .substring(0, entity.getFilename().lastIndexOf("."));
      String ext = entity.getFilename().substring(entity.getFilename().lastIndexOf(".") + 1);

      File initialFile = null;
      Path tmpDir = null;
      Path resultFile = null;
      try {
        initialFile = File.createTempFile(entity.getId() + "-", "." + ext);

        tmpDir = Files.createTempDirectory(null);
        Files.write(initialFile.toPath(), fileContent);

        logger().trace(loggerPrefix + "Exec : " + appProperties.getLibreoffice().getPath()
            + " --convert-to pdf --outdir " + tmpDir.toString() + " " + initialFile.getPath());

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
        BufferedReader stdInput = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(
            new InputStreamReader(process.getErrorStream()));
        int exitCode = process.waitFor();
        if (exitCode == 0) {
          String targetFile = initialFile.getName()
              .substring(0, initialFile.getName().lastIndexOf("."));

          resultFile = Path.of(tmpDir.toString(), targetFile + ".pdf");
          fileContent = Files.readAllBytes(resultFile);
          logger().debug(loggerPrefix + "Converted : " + entity.getMimeType());
        } else {
          logger().error(loggerPrefix + "Cannot convert, exit code = " + exitCode);

          String s;
          StringBuilder strBuilder = new StringBuilder();
          while ((s = stdInput.readLine()) != null) {
            strBuilder.append(s).append("\n");
          }
          if (strBuilder.length() > 0) {
            logger().error(
                loggerPrefix + "StdInput : " + strBuilder.substring(0, strBuilder.length() - 2));
          }

          strBuilder = new StringBuilder();
          while ((s = stdError.readLine()) != null) {
            strBuilder.append(s).append("\n");
          }
          if (strBuilder.length() > 0) {
            logger().error(
                loggerPrefix + "StdError : " + strBuilder.substring(0, strBuilder.length() - 2));
          }

          fileContent = null;
        }
      } catch (IOException | InterruptedException e) {
        logger().error(loggerPrefix + "Cannot convert, exception = " + e.getMessage());
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
      logger().debug(loggerPrefix + "Image or PDF content, skip (" + entity.getMimeType() + ")");
      fileContent = null;
    }
    return fileContent;
  }
}
