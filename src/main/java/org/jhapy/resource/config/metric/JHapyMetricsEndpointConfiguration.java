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

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.jhapy.resource.endpoint.JHapyMetricsEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>JHapyMetricsEndpointConfiguration class.</p>
 */
@Configuration
@ConditionalOnClass(Timed.class)
@AutoConfigureAfter(MetricsEndpointAutoConfiguration.class)
public class JHapyMetricsEndpointConfiguration {

  /**
   * <p>jHapyMetricsEndpoint.</p>
   *
   * @param meterRegistry a {@link MeterRegistry} object.
   * @return a {@link JHapyMetricsEndpoint} object.
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnAvailableEndpoint
  public JHapyMetricsEndpoint jHapyMetricsEndpoint(MeterRegistry meterRegistry) {
    return new JHapyMetricsEndpoint(meterRegistry);
  }
}