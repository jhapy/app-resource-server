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

import org.apache.commons.text.diff.DeleteCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.MetaData;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.cqrs.command.AbstractBaseCommand;
import org.jhapy.cqrs.command.CreateEntityCommand;
import org.jhapy.cqrs.command.DeleteEntityCommand;
import org.jhapy.cqrs.command.UpdateEntityCommand;
import org.jhapy.cqrs.query.AbstractCountAnyMatchingQuery;
import org.jhapy.cqrs.query.AbstractFindAnyMatchingQuery;
import org.jhapy.cqrs.query.AbstractGetAllGenericQuery;
import org.jhapy.cqrs.query.AbstractGetByIdGenericQuery;
import org.jhapy.dto.domain.BaseEntityUUIDId;
import org.jhapy.dto.serviceQuery.BaseRemoteQuery;
import org.jhapy.dto.serviceQuery.ServiceResult;
import org.jhapy.dto.serviceQuery.generic.*;
import org.jhapy.dto.utils.PageDTO;
import org.jhapy.dto.utils.Pageable;
import org.jhapy.resource.domain.BaseEntity;
import org.reflections.Reflections;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public abstract class BaseEndpoint<T extends BaseEntity, D extends BaseEntityUUIDId>
    implements HasLogger {

  protected final CommandGateway commandGateway;
  protected final QueryGateway queryGateway;

  protected BaseEndpoint(CommandGateway commandGateway, QueryGateway queryGateway) {
    this.commandGateway = commandGateway;
    this.queryGateway = queryGateway;
  }

  protected Map<String, Object> getContext(BaseRemoteQuery query) {
    Map<String, Object> context = new HashMap<>();

    context.put("username", query.getQueryUsername());
    context.put("userId", query.getQueryUserId());
    context.put("sessionId", query.getQuerySessionId());
    context.put("iso3Language", query.getQueryIso3Language());
    context.put("currentPosition", query.getQueryCurrentPosition());
    context.put("clientId", query.getQueryExternalClientID());
    return context;
  }

  protected MetaData buildMetaData(BaseRemoteQuery query) {
    return MetaData.from(getContext(query));
  }

  protected ResponseEntity<ServiceResult> handleResult(String loggerPrefix) {
    return handleResult(loggerPrefix, new ServiceResult<>());
  }

  protected ResponseEntity<ServiceResult> handleResult(String loggerPrefix, Object result) {
    return handleResult(loggerPrefix, new ServiceResult<>(result));
  }

  protected ResponseEntity<ServiceResult> handleResult(String loggerPrefix, String error) {
    return handleResult(loggerPrefix, new ServiceResult<>(error));
  }

  protected ResponseEntity<ServiceResult> handleResult(String loggerPrefix, ServiceResult result) {
    if (result.getIsSuccess()) {
      ResponseEntity<ServiceResult> response = ResponseEntity.ok(result);
      if (logger().isTraceEnabled()) {
        debug(loggerPrefix, "Response OK : {0}", result);
      }
      return response;
    } else {
      error(loggerPrefix, "Response KO : {0}", result.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  protected Pageable convert(org.springframework.data.domain.Pageable domain) {
    if (domain.isUnpaged()) return null;
    var result = new Pageable();
    result.setSize(domain.getPageSize());
    result.setOffset((int) domain.getOffset());
    result.setPage(domain.getPageNumber());
    result.setSort(
        domain.getSort().stream()
            .map(
                order -> {
                  var o = new Pageable.Order();
                  o.setProperty(order.getProperty());
                  o.setDirection(
                      order.getDirection().isAscending()
                          ? Pageable.Order.Direction.ASC
                          : Pageable.Order.Direction.DESC);
                  return o;
                })
            .collect(Collectors.toSet()));
    return result;
  }

  protected PageDTO<D> toDtoPage(Page<T> domain, List<D> data) {
    PageDTO<D> result = new PageDTO<>();
    result.setTotalPages(domain.getTotalPages());
    result.setSize(domain.getSize());
    result.setNumber(domain.getNumber());
    result.setNumberOfElements(domain.getNumberOfElements());
    result.setTotalElements(domain.getTotalElements());
    result.setPageable(convert(domain.getPageable()));
    result.setContent(data);
    return result;
  }

  @PostMapping(value = "/findAnyMatching")
  public ResponseEntity<ServiceResult> findAnyMatching(@RequestBody FindAnyMatchingQuery query) {
    var loggerPrefix = getLoggerPrefix("findAnyMatching");

    Class<? extends AbstractFindAnyMatchingQuery<D>> findAnyMatchingQueryClass =
        (Class<? extends AbstractFindAnyMatchingQuery<D>>)
            lookupChildClass(AbstractFindAnyMatchingQuery.class, dClass());
    if (findAnyMatchingQueryClass == null) {
      return handleResult(
          loggerPrefix,
          String.format(
              "Cannot find correct FindAnyMatchingQuery query for %s", dClass().getSimpleName()));
    }
    AbstractFindAnyMatchingQuery<D> findAnyMatchingQuery = newInstance(findAnyMatchingQueryClass);
    if (findAnyMatchingQuery == null) {
      return handleResult(
          loggerPrefix,
          String.format(
              "Cannot create new Instance of FindAnyMatchingQuery for %s",
              dClass().getSimpleName()));
    }

    findAnyMatchingQuery.setFilter(query.getFilter());
    findAnyMatchingQuery.setShowInactive(query.getShowInactive());
    findAnyMatchingQuery.setPageable(query.getPageable());

    return handleResult(
        loggerPrefix, queryGateway.query(findAnyMatchingQuery, PageDTO.class).join());
  }

  @PostMapping(value = "/countAnyMatching")
  public ResponseEntity<ServiceResult> countAnyMatching(@RequestBody CountAnyMatchingQuery query) {
    var loggerPrefix = getLoggerPrefix("countAnyMatching");

    Class<?> countAnyMatchingQueryClass =
        lookupChildClass(AbstractCountAnyMatchingQuery.class, dClass());
    if (countAnyMatchingQueryClass == null) {
      return handleResult(
          loggerPrefix,
          String.format(
              "Cannot find correct CountAnyMatchingQuery query for %s", dClass().getSimpleName()));
    }
    AbstractCountAnyMatchingQuery<D> countAnyMatchingQuery =
        (AbstractCountAnyMatchingQuery<D>) newInstance(countAnyMatchingQueryClass);
    if (countAnyMatchingQuery == null) {
      return handleResult(
          loggerPrefix,
          String.format(
              "Cannot create new Instance of CountAnyMatchingQuery for %s",
              dClass().getSimpleName()));
    }

    countAnyMatchingQuery.setFilter(query.getFilter());
    countAnyMatchingQuery.setShowInactive(query.getShowInactive());

    return handleResult(loggerPrefix, queryGateway.query(countAnyMatchingQuery, Long.class).join());
  }

  @PostMapping(value = "/getById")
  public ResponseEntity<ServiceResult> getById(@Valid @RequestBody GetByIdQuery query) {
    var loggerPrefix = getLoggerPrefix("getById");

    debug(loggerPrefix, "ID = {0}", query.getId());

    Class<?> getByIdQueryClass = lookupChildClass(AbstractGetByIdGenericQuery.class, dClass());
    if (getByIdQueryClass == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot find correct GetById query for %s", dClass().getSimpleName()));
    }
    AbstractGetByIdGenericQuery<D> getByIdQuery =
        (AbstractGetByIdGenericQuery<D>) newInstance(getByIdQueryClass, UUID.class, query.getId());
    if (getByIdQuery == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot create new Instance of GetById for %s", dClass().getSimpleName()));
    }

    return handleResult(loggerPrefix, queryGateway.query(getByIdQuery, dClass()).join());
  }

  @PostMapping(value = "/getAll")
  public ResponseEntity<ServiceResult> getAll(@RequestBody BaseRemoteQuery query) {
    var loggerPrefix = getLoggerPrefix("getAll");

    Class<? extends AbstractGetAllGenericQuery<D>> getAllQueryClass =
        (Class<? extends AbstractGetAllGenericQuery<D>>)
            lookupChildClass(AbstractGetAllGenericQuery.class, dClass());
    if (getAllQueryClass == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot find correct GetAll query for %s", dClass().getSimpleName()));
    }
    AbstractGetAllGenericQuery<D> getAllQuery = newInstance(getAllQueryClass);
    if (getAllQuery == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot create new Instance of GetAll for %s", dClass().getSimpleName()));
    }

    return handleResult(loggerPrefix, queryGateway.query(getAllQuery, List.class).join());
  }

  @PostMapping(value = "/save")
  public ResponseEntity<ServiceResult> save(@Valid @RequestBody SaveQuery<D> query) {
    var loggerPrefix = getLoggerPrefix("save");

    UUID id;
    Class<? extends AbstractBaseCommand> commandClass;
    if (query.getEntity().getId() == null) {
      id = UUID.randomUUID();
      commandClass = CreateEntityCommand.class;
    } else {
      id = query.getEntity().getId();
      commandClass = UpdateEntityCommand.class;
    }
    Class<?> getByIdQueryClass = lookupChildClass(AbstractGetByIdGenericQuery.class, dClass());
    if (getByIdQueryClass == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot find correct GetById query for %s", dClass().getSimpleName()));
    }
    AbstractGetByIdGenericQuery<D> getByIdQuery =
        (AbstractGetByIdGenericQuery<D>) newInstance(getByIdQueryClass, UUID.class, id);
    if (getByIdQuery == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot create new Instance of GetById for %s", dClass().getSimpleName()));
    }
    getByIdQuery.setId(id);
    Class<?> childCommandClass = lookupChildClass(commandClass, dClass());
    if (childCommandClass == null) {
      return handleResult(
          loggerPrefix,
          String.format(
              "Cannot find correct %s child command for %s",
              commandClass.getSimpleName(), dClass().getSimpleName()));
    }

    try (SubscriptionQueryResult<D, D> subscriptionQueryResult =
        queryGateway.subscriptionQuery(getByIdQuery, dClass(), dClass())) {

      AbstractBaseCommand command =
          (AbstractBaseCommand) newInstance(childCommandClass, dClass(), query.getEntity());
      if (command == null) {
        return handleResult(
            loggerPrefix,
            String.format(
                "Cannot create new Instance of %s for %s",
                childCommandClass.getSimpleName(), dClass().getSimpleName()));
      }
      commandGateway.sendAndWait(command);

      return handleResult(
          loggerPrefix, subscriptionQueryResult.updates().blockFirst(Duration.ofSeconds(30)));
    }
  }

  @PostMapping(value = "/saveAll")
  public ResponseEntity<ServiceResult> saveAll(@Valid @RequestBody SaveAllQuery<D> query) {
    var loggerPrefix = getLoggerPrefix("saveAll");

    StringBuilder errorMessages = new StringBuilder();
    List<AbstractBaseCommand> commands = new ArrayList<>();
    List<UUID> uuids = new ArrayList<>();
    query
        .getEntity()
        .forEach(
            entity -> {
              Class<? extends AbstractBaseCommand> commandClass;
              if (entity.getId() == null) {
                uuids.add(UUID.randomUUID());
                commandClass = CreateEntityCommand.class;
              } else {
                uuids.add(entity.getId());
                commandClass = UpdateEntityCommand.class;
              }

              Class<? extends AbstractBaseCommand> childCommandClass =
                  lookupChildClass(commandClass, dClass());
              if (childCommandClass == null) {
                errorMessages.append(
                    String.format(
                        "Cannot find correct %s child command for %s%n",
                        commandClass.getSimpleName(), dClass().getSimpleName()));
              } else {
                AbstractBaseCommand command = newInstance(childCommandClass, dClass(), entity);
                if (command == null) {
                  errorMessages.append(
                      String.format(
                          "Cannot create new Instance of %s for %s%n",
                          childCommandClass.getSimpleName(), dClass().getSimpleName()));
                } else {
                  commands.add(command);
                }
              }
            });
    if (errorMessages.length() > 0) {
      return handleResult(loggerPrefix, errorMessages.toString());
    }

    Class<?> getByIdQueryClass = lookupChildClass(AbstractGetByIdGenericQuery.class, dClass());
    if (getByIdQueryClass == null) {
      return handleResult(
          loggerPrefix,
          String.format("Cannot find correct GetById query for %s%n", dClass().getSimpleName()));
    }

    List<D> result = new ArrayList<>();

    for (int i = 0; i < commands.size(); i++) {
      AbstractGetByIdGenericQuery<D> getByIdQuery =
          (AbstractGetByIdGenericQuery<D>) newInstance(getByIdQueryClass, UUID.class, uuids.get(i));
      if (getByIdQuery == null) {
        return handleResult(
            loggerPrefix,
            String.format(
                "Cannot create new Instance of GetById for %s", dClass().getSimpleName()));
      }
      getByIdQuery.setId(uuids.get(i));
      try (SubscriptionQueryResult<D, D> subscriptionQueryResult =
          queryGateway.subscriptionQuery(getByIdQuery, dClass(), dClass())) {
        commandGateway.sendAndWait(commands.get(i));
        result.add(subscriptionQueryResult.updates().blockFirst(Duration.ofSeconds(30)));
      }
    }

    return handleResult(loggerPrefix, result);
  }

  @PostMapping(value = "/delete")
  public ResponseEntity<ServiceResult> delete(@Valid @RequestBody DeleteByIdQuery query) {
    var loggerPrefix = getLoggerPrefix("delete");

    Class<? extends DeleteEntityCommand> childCommandClass =
        lookupChildClass(DeleteEntityCommand.class, dClass());
    if (childCommandClass == null) {
      return handleResult(
          loggerPrefix,
          String.format(
              "Cannot find correct %s child command for %s",
              DeleteCommand.class.getSimpleName(), dClass().getSimpleName()));
    }
    DeleteEntityCommand command = newInstance(childCommandClass, UUID.class, query.getId());
    commandGateway.sendAndWait(command);
    return handleResult(loggerPrefix);
  }

  protected Class<T> tClass() {
    ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();

    return (Class<T>) superclass.getActualTypeArguments()[0];
  }

  protected Class<D> dClass() {
    ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();

    return (Class<D>) superclass.getActualTypeArguments()[1];
  }

  protected <P> Class<? extends P> lookupChildClass(
      Class<P> parentClass, Class<?> parameterizedType) {
    Reflections reflections = new Reflections("org.jhapy.cqrs");

    Set<Class<? extends P>> subClasses = reflections.getSubTypesOf(parentClass);
    return subClasses.stream()
        .map(
            aClass -> {
              ParameterizedType superclass = (ParameterizedType) aClass.getGenericSuperclass();
              if (superclass.getActualTypeArguments()[0].equals(parameterizedType)) return aClass;
              else return null;
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  protected <P> P newInstance(Class<P> aClass) {
    String loggerPrefix = getLoggerPrefix("newInstance");
    try {
      return aClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      error(
          loggerPrefix,
          e,
          "Cannot create new Instance of {0} : {1}",
          aClass.getSimpleName(),
          e.getMessage());
      return null;
    }
  }

  protected <P> P newInstance(Class<P> aClass, Class<?> parameterClass, Object parameterValue) {
    String loggerPrefix = getLoggerPrefix("newInstance");
    try {
      return aClass.getDeclaredConstructor(parameterClass).newInstance(parameterValue);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      error(
          loggerPrefix,
          e,
          "Cannot create new Instance of {0} : {1}",
          aClass.getSimpleName(),
          e.getMessage());
      return null;
    }
  }
}
