package org.jhapy.resource.query;

import org.apache.commons.lang3.StringUtils;
import org.jhapy.commons.security.SecurityUtils;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.dto.domain.BaseEntityUUIDId;
import org.jhapy.dto.utils.PageDTO;
import org.jhapy.resource.converter.GenericMapper;
import org.jhapy.resource.domain.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public interface BaseQueryHandler<E extends BaseEntity, D extends BaseEntityUUIDId>
    extends HasLogger {
  MongoRepository<E, UUID> getRepository();

  MongoTemplate getMongoTemplate();

  Class<E> getEntityClass();

  GenericMapper<E, D> getConverter();

  default PageDTO<D> toDtoPage(Page<E> domain, List<D> data) {
    PageDTO<D> result = new PageDTO<>();
    result.setTotalPages(domain.getTotalPages());
    result.setSize(domain.getSize());
    result.setNumber(domain.getNumber());
    result.setNumberOfElements(domain.getNumberOfElements());
    result.setTotalElements(domain.getTotalElements());
    result.setPageable(getConverter().convert(domain.getPageable()));
    result.setContent(data);
    return result;
  }

  default Page<E> findAnyMatching(
      String filter, Boolean showInactive, Pageable pageable, Object... otherCriteria) {
    var loggerString = getLoggerPrefix("findAnyMatching");

    debug(loggerString, "----------------------------------");

    debug(
        loggerString,
        "In, Entity = {0}, Filter = {1}, Show Inactive = {2}, Pageable = {3}",
        getEntityClass().getSimpleName(),
        filter,
        showInactive,
        pageable);

    Criteria criteria = new Criteria();

    buildSearchQuery(criteria, filter, showInactive);

    Query query = new Query(criteria);
    if (pageable.isPaged()) {
      query.with(pageable);
    }

    List<E> result = getMongoTemplate().find(query, getEntityClass());

    long nbRecords = getMongoTemplate().count(Query.of(query).limit(-1).skip(-1), getEntityClass());

    Page<E> pagedResult;

    if (pageable.isPaged()) {
      pagedResult = PageableExecutionUtils.getPage(result, pageable, () -> nbRecords);
    } else {
      pagedResult = new PageImpl<>(result, pageable, nbRecords);
    }

    logger()
        .debug(
            loggerString
                + "Out : Elements = "
                + pagedResult.getContent().size()
                + " of "
                + pagedResult.getTotalElements()
                + ", Page = "
                + pagedResult.getNumber()
                + " of "
                + pagedResult.getTotalPages());

    return pagedResult;
  }

  default long countAnyMatching(String filter, Boolean showInactive, Object... otherCriteria) {
    var loggerPrefix = getLoggerPrefix("countAnyMatching");

    logger().debug(loggerPrefix + "----------------------------------");

    String currentUser = SecurityUtils.getCurrentUserLogin().orElse(null);

    logger()
        .debug(
            loggerPrefix
                + "In, Filter = "
                + filter
                + ", User = "
                + currentUser
                + ", Show Inactive = "
                + showInactive);

    Criteria criteria = new Criteria();

    buildSearchQuery(criteria, filter, showInactive);

    Query query = new Query(criteria);

    logger().debug(loggerPrefix + "Query = " + query);

    long nbRecords = getMongoTemplate().count(query, getEntityClass());

    logger().debug(loggerPrefix + "Out = " + nbRecords + " items");

    return nbRecords;
  }

  default void buildSearchQuery(Criteria rootCriteria, String filter, Boolean showInactive) {
    List<Criteria> orPredicates = new ArrayList<>();
    List<Criteria> andPredicated = new ArrayList<>();

    if (StringUtils.isNotBlank(filter)) {
      Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
      orPredicates.add(Criteria.where("name").regex(pattern));
      orPredicates.add(Criteria.where("description").regex(pattern));
    }

    if (showInactive == null || !showInactive) {
      andPredicated.add(Criteria.where("isActive").is(Boolean.TRUE));
    }

    if (!orPredicates.isEmpty()) {
      andPredicated.add(new Criteria().orOperator(orPredicates.toArray(new Criteria[0])));
    }

    if (!andPredicated.isEmpty()) {
      rootCriteria.andOperator(andPredicated.toArray(new Criteria[0]));
    }
  }
}
