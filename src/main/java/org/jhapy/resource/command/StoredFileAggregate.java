package org.jhapy.resource.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.jhapy.cqrs.command.AbstractBaseAggregate;
import org.jhapy.cqrs.command.resource.CreateStoredFileCommand;
import org.jhapy.cqrs.command.resource.DeleteStoredFileCommand;
import org.jhapy.cqrs.command.resource.UpdateStoredFileCommand;
import org.jhapy.cqrs.event.resource.StoredFileCreatedEvent;
import org.jhapy.cqrs.event.resource.StoredFileDeletedEvent;
import org.jhapy.cqrs.event.resource.StoredFileUpdatedEvent;
import org.jhapy.resource.converter.StoredFileConverter;
import org.jhapy.resource.domain.PdfConvertEnum;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

@Aggregate
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class StoredFileAggregate extends AbstractBaseAggregate {
  private String filename;

  private String mimeType;

  private long filesize;

  private byte[] content;
  private String contentFileId;

  private byte[] md5Content;

  private byte[] originalContent;
  private String originalContentFileId;

  private PdfConvertEnum pdfConvertStatus;
  private byte[] pdfContent;
  private String pdfContentFileId;

  private Map<String, String> metadata = new HashMap<>();

  private UUID relatedObjectId;
  private String relatedObjectClass;

  private transient StoredFileConverter converter;

  @CommandHandler
  public StoredFileAggregate(
      @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
          CreateStoredFileCommand command,
      @Autowired StoredFileConverter converter) {
    this.converter = converter;

    StoredFileCreatedEvent event = converter.toStoredFileCreatedEvent(command.getEntity());
    event.setId(command.getId());
    AggregateLifecycle.apply(event);
  }

  @Autowired
  public void setConverter(StoredFileConverter converter) {
    this.converter = converter;
  }

  @CommandHandler
  public void handle(UpdateStoredFileCommand command) {
    StoredFileUpdatedEvent event = converter.toStoredFileUpdatedEvent(command.getEntity());
    AggregateLifecycle.apply(event);
  }

  @CommandHandler
  public void handle(DeleteStoredFileCommand command) {
    StoredFileDeletedEvent event = new StoredFileDeletedEvent(command.getId());
    AggregateLifecycle.apply(event);
  }

  @EventSourcingHandler
  public void on(StoredFileCreatedEvent event) {
    converter.updateAggregateFromStoredFileCreatedEvent(event, this);
  }

  @EventSourcingHandler
  public void on(StoredFileUpdatedEvent event) {
    converter.updateAggregateFromStoredFileUpdatedEvent(event, this);
  }

  @EventSourcingHandler
  public void on(StoredFileDeletedEvent event) {
    markDeleted();
  }
}
