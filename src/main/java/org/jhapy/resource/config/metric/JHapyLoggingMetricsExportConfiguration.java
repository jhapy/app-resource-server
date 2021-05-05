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

package org.jhapy.resource.config.metric;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import java.util.concurrent.TimeUnit;
import org.jhapy.commons.config.AppProperties;
import org.jhapy.commons.utils.HasLogger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Console Reporter Configuration
 * <p>
 * Pass the metrics to the logs with Dropwizard Reporter implementation see
 * https://github.com/micrometer-metrics/micrometer-docs/blob/9fedeb5/src/docs/guide/console-reporter.adoc
 */
@Configuration
@ConditionalOnProperty("jhapy.metrics.logs.enabled")
public class JHapyLoggingMetricsExportConfiguration implements HasLogger {

  private final AppProperties appProperties;

  /**
   * <p>Constructor for JHapyLoggingMetricsExportConfiguration.</p>
   *
   * @param appProperties a {@link AppProperties} object.
   */
  public JHapyLoggingMetricsExportConfiguration(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  /**
   * <p>dropwizardRegistry.</p>
   *
   * @return a {@link com.codahale.metrics.MetricRegistry} object.
   */
  @Bean
  public MetricRegistry dropwizardRegistry() {
    return new MetricRegistry();
  }

  /**
   * <p>consoleReporter.</p>
   *
   * @param dropwizardRegistry a {@link com.codahale.metrics.MetricRegistry} object.
   * @return a {@link com.codahale.metrics.Slf4jReporter} object.
   */
  @Bean
  public Slf4jReporter consoleReporter(MetricRegistry dropwizardRegistry) {
    var loggerPrefix = getLoggerPrefix("consoleReporter");
    logger().info(loggerPrefix + "Initializing Metrics Log reporting");
    Marker metricsMarker = MarkerFactory.getMarker("metrics");
    final Slf4jReporter reporter = Slf4jReporter.forRegistry(dropwizardRegistry)
        .outputTo(LoggerFactory.getLogger("metrics"))
        .markWith(metricsMarker)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
    reporter.start(appProperties.getMetrics().getLogs().getReportFrequency(), TimeUnit.SECONDS);
    return reporter;
  }

  // Needed to enable the Console reporter
  // https://github.com/micrometer-metrics/micrometer-docs/blob/9fedeb5/src/docs/guide/console-reporter.adoc

  /**
   * <p>consoleLoggingRegistry.</p>
   *
   * @param dropwizardRegistry a {@link com.codahale.metrics.MetricRegistry} object.
   * @return a {@link MeterRegistry} object.
   */
  @Bean
  public MeterRegistry consoleLoggingRegistry(MetricRegistry dropwizardRegistry) {
    DropwizardConfig dropwizardConfig = new DropwizardConfig() {
      @Override
      public String prefix() {
        return "console";
      }

      @Override
      public String get(String key) {
        return null;
      }
    };

    return new DropwizardMeterRegistry(dropwizardConfig, dropwizardRegistry,
        HierarchicalNameMapper.DEFAULT, Clock.SYSTEM) {
      @Override
      protected Double nullGaugeValue() {
        return null;
      }
    };
  }
}