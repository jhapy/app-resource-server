package org.jhapy.resource.config;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.TypeHierarchyPermission;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.interceptors.LoggingInterceptor;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.jhapy.cqrs.query.AbstractBaseQuery;
import org.jhapy.dto.domain.BaseEntity;
import org.jhapy.dto.serviceQuery.BaseRemoteQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {

  @Bean
  public LoggingInterceptor<Message<?>> loggingInterceptor() {
    return new LoggingInterceptor<>();
  }

  @Autowired
  public void configureLoggingInterceptorFor(
      CommandBus commandBus, LoggingInterceptor<Message<?>> loggingInterceptor) {
    commandBus.registerDispatchInterceptor(loggingInterceptor);
    commandBus.registerHandlerInterceptor(loggingInterceptor);
  }

  @Autowired
  public void configureLoggingInterceptorFor(
      EventBus eventBus, LoggingInterceptor<Message<?>> loggingInterceptor) {
    eventBus.registerDispatchInterceptor(loggingInterceptor);
  }

  @Autowired
  public void configureLoggingInterceptorFor(
      EventProcessingConfigurer eventProcessingConfigurer,
      LoggingInterceptor<Message<?>> loggingInterceptor) {
    eventProcessingConfigurer.registerDefaultHandlerInterceptor(
        (config, processorName) -> loggingInterceptor);
  }

  @Autowired
  public void configureLoggingInterceptorFor(
      QueryBus queryBus, LoggingInterceptor<Message<?>> loggingInterceptor) {
    queryBus.registerDispatchInterceptor(loggingInterceptor);
    queryBus.registerHandlerInterceptor(loggingInterceptor);
  }

  @Autowired
  public void configureErrorHandlers(EventProcessingConfigurer configurer) {
    configurer.registerListenerInvocationErrorHandler(
        "stored-file-group", configuration -> PropagatingErrorHandler.instance());
  }

  @Bean
  @Qualifier("messageSerializer")
  public Serializer messageSerializer() {
    XStream xStream = new XStream();
    xStream.addPermission(new TypeHierarchyPermission(BaseEntity.class));
    xStream.addPermission(new TypeHierarchyPermission(BaseRemoteQuery.class));
    xStream.addPermission(new TypeHierarchyPermission(AbstractBaseQuery.class));
    xStream.addPermission(
        new WildcardTypePermission(new String[] {"org.jhapy.dto.**", "org.jhapy.cqrs.**"}));

    return XStreamSerializer.builder().xStream(xStream).build();
  }
}
