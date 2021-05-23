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

package org.jhapy.resource.aop.logging;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.commons.utils.SpringProfileConstants;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Aspect for logging execution of service and repository Spring components.
 *
 * By default, it only runs with the "dev" profile.
 */
public class LoggingAspect implements HasLogger {

  private final Environment env;

  public LoggingAspect(Environment env) {
    this.env = env;
  }

  /**
   * Pointcut that matches all repositories, services and Web REST endpoints.
   */
  @Pointcut("within(@org.springframework.stereotype.Repository *)" +
      " || within(@org.springframework.stereotype.Service *)" +
      " || within(@org.springframework.web.bind.annotation.RestController *)")
  public void springBeanPointcut() {
    // Method is empty as this is just a Pointcut, the implementations are in the advices.
  }

  /**
   * Pointcut that matches all Spring beans in the application's main packages.
   */
  @Pointcut("target(org.jhapy.resource.service.CrudService)")
  public void serviceEndpoint() {
    // Method is empty as this is just a Pointcut, the implementations are in the advices.
  }

  @Pointcut("!execution(@org.jhapy.resource.aop.logging.NoLog * *(..))")
  public void methodAnnotatedWithNoLog() {
  }

  /**
   * Advice that logs methods throwing exceptions.
   *
   * @param joinPoint join point for advice.
   * @param e exception.
   */
  @AfterThrowing(pointcut = "serviceEndpoint() && springBeanPointcut()", throwing = "e")
  public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
    var loggerPrefix = getLoggerPrefix(joinPoint.getSignature().getName());
    Class sourceClass = joinPoint.getSignature().getDeclaringType();

    if (env.acceptsProfiles(Profiles.of(SpringProfileConstants.SPRING_PROFILE_DEVELOPMENT,
        SpringProfileConstants.SPRING_PROFILE_DEVELOPMENT_LOCAL))) {
      logger(sourceClass)
          .error(loggerPrefix + ">>> Exception in {} with cause = '{}' and exception = '{}'",
              joinPoint.getSignature().toShortString(),
              e.getCause() != null ? e.getCause() : "NULL",
              e.getMessage(), e);

    } else {
      logger(sourceClass).error(loggerPrefix + ">>> Exception in {} with cause = {}",
          joinPoint.getSignature().toShortString(), e.getCause() != null ? e.getCause() : "NULL");
    }
  }

  /**
   * Advice that logs when a method is entered and exited.
   *
   * @param joinPoint join point for advice.
   * @return result.
   * @throws Throwable throws {@link IllegalArgumentException}.
   */
  @Around("serviceEndpoint() && springBeanPointcut() && methodAnnotatedWithNoLog()")
  public Object logAroundServiceOrEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
    var loggerPrefix = getLoggerPrefix(joinPoint.getSignature().getName());
    Class sourceClass = joinPoint.getSignature().getDeclaringType();

    // TODO : Add an endpoint to activate/deactivate at runtime
    if (logger(sourceClass).isTraceEnabled()) {
      logger(sourceClass).debug(loggerPrefix + ">>> Enter with argument[s] = {}",
          Arrays.toString(joinPoint.getArgs()));
    } else if (logger().isDebugEnabled()) {
      logger(sourceClass).debug(loggerPrefix + ">>> Enter");
    }
    try {
      long start = System.currentTimeMillis();
      Object result = joinPoint.proceed();
      long end = System.currentTimeMillis();
      long duration = end - start;
      if (duration > 1000) {
        logger(sourceClass).warn(
            loggerPrefix + ">>> Service or EndPoint Method: " + joinPoint.getSignature().getName()
                + " took long ... "
                + duration + " ms");
      }
      if (logger().isTraceEnabled()) {
        logger(sourceClass)
            .debug(loggerPrefix + ">>> Exit, duration {} ms with result = {}", duration, result);
      } else if (logger().isDebugEnabled()) {
        logger(sourceClass).debug(loggerPrefix + ">>> Exit, duration {} ms", duration);
      }
      return result;
    } catch (IllegalArgumentException e) {
      logger(sourceClass).error(loggerPrefix + ">>> Illegal argument: {} in {}",
          Arrays.toString(joinPoint.getArgs()),
          joinPoint.getSignature().toShortString());

      throw e;
    }
  }

  @Around("springBeanPointcut() && methodAnnotatedWithNoLog()")
  public Object logAroundRepository(ProceedingJoinPoint joinPoint) throws Throwable {
    var loggerPrefix = getLoggerPrefix(joinPoint.getSignature().getName());
    Class sourceClass = joinPoint.getSignature().getDeclaringType();

    try {
      long start = System.currentTimeMillis();
      Object result = joinPoint.proceed();
      long end = System.currentTimeMillis();
      long duration = end - start;
      if (duration > 1000) {
        logger(sourceClass).warn(
            loggerPrefix + ">>> Repository Method: " + joinPoint.getSignature().getName()
                + " took long ... "
                + duration + " ms");
      }
      return result;
    } catch (IllegalArgumentException e) {
      logger(sourceClass).error(loggerPrefix + ">>> Illegal argument: {} in {}",
          Arrays.toString(joinPoint.getArgs()),
          joinPoint.getSignature().toShortString());

      throw e;
    }
  }
}
