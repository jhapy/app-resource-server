package org.jhapy.resource.config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.commons.utils.SpringProfileConstants;

@Profile(SpringProfileConstants.SPRING_PROFILE_DEVELOPMENT_EUREKA)
@Configuration
public class DockerEurekaClientConfiguration implements
    HasLogger {

  private final ConfigurableEnvironment env;

  public DockerEurekaClientConfiguration(ConfigurableEnvironment env) {
    this.env = env;
  }

  @Bean
  @Primary
  public EurekaClientConfigBean eurekaClientConfigBean(ConfigurableEnvironment env) {
    EurekaClientConfigBean client = new EurekaClientConfigBean();
    if ("bootstrap".equals(this.env.getProperty("spring.config.name"))) {
      client.setRegisterWithEureka(false);
    }

    return client;
  }

  @Bean
  @Primary
  public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
      ManagementMetadataProvider managementMetadataProvider) {
    String loggerPrefix = getLoggerPrefix("eurekaInstanceConfigBean");

    String hostname = env.getProperty("eureka.instance.hostname");
    boolean preferIpAddress = Boolean
        .parseBoolean(env.getProperty("eureka.instance.prefer-ip-address"));
    String ipAddress = env.getProperty("eureka.instance.ip-address");
    boolean isSecurePortEnabled = Boolean
        .parseBoolean(env.getProperty("eureka.instance.secure-port-enabled"));
    String serverContextPath = this.env.getProperty("server.servlet.context-path", "/");
    int serverPort = Integer
        .valueOf(this.env.getProperty("server.port", this.env.getProperty("port", "8080")));
    Integer managementPort = this.env
        .getProperty("management.server.port", Integer.class);
    String managementContextPath = this.env.getProperty("management.server.servlet.context-path");
    Integer jmxPort = this.env
        .getProperty("com.sun.management.jmxremote.port", Integer.class);
    EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);
    instance.setNonSecurePort(serverPort);
    instance.setInstanceId(IdUtils.getDefaultInstanceId(this.env));
    instance.setPreferIpAddress(preferIpAddress);
    instance.setSecurePortEnabled(isSecurePortEnabled);
    if (StringUtils.hasText(ipAddress)) {
      instance.setIpAddress(ipAddress);
    }

    if (isSecurePortEnabled) {
      instance.setSecurePort(serverPort);
    }

    if (StringUtils.hasText(hostname)) {
      instance.setHostname(hostname);
    }

    String statusPageUrlPath = env.getProperty("eureka.instance.status-page-url-path");
    String healthCheckUrlPath = env.getProperty("eureka.instance.health-check-url-path");
    if (StringUtils.hasText(statusPageUrlPath)) {
      instance.setStatusPageUrlPath(statusPageUrlPath);
    }

    if (StringUtils.hasText(healthCheckUrlPath)) {
      instance.setHealthCheckUrlPath(healthCheckUrlPath);
    }

    ManagementMetadata metadata = managementMetadataProvider
        .get(instance, serverPort, serverContextPath, managementContextPath, managementPort);
    if (metadata != null) {
      instance.setStatusPageUrl(metadata.getStatusPageUrl());
      instance.setHealthCheckUrl(metadata.getHealthCheckUrl());
      if (instance.isSecurePortEnabled()) {
        instance.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
      }

      Map<String, String> metadataMap = instance.getMetadataMap();
      metadataMap.computeIfAbsent("management.port", (k) -> {
        return String.valueOf(metadata.getManagementPort());
      });
    } else if (StringUtils.hasText(managementContextPath)) {
      instance.setHealthCheckUrlPath(managementContextPath + instance.getHealthCheckUrlPath());
      instance.setStatusPageUrlPath(managementContextPath + instance.getStatusPageUrlPath());
    }

    this.setupJmxPort(instance, jmxPort);

    EurekaInstanceConfigBean result = null;
    int nbLoop = 1;
    while (result == null) {
      logger().info(loggerPrefix + "Loop " + nbLoop++);
      try {
        List<String> servers = eurekaClientConfigBean(env).getEurekaServerServiceUrls(null);
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        external_loop:
        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
          if (networkInterface.getName().startsWith("lo")) {
            continue;
          }
          for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            if (interfaceAddress.getAddress() instanceof Inet4Address) {
              logger().info(loggerPrefix +
                      "Interface {}: {}/{}",
                  networkInterface.getName(),
                  interfaceAddress.getAddress(),
                  interfaceAddress.getNetworkPrefixLength()
              );
              SubnetUtils subnet = new SubnetUtils(
                  interfaceAddress.getAddress().getHostAddress() +
                      "/" + interfaceAddress.getNetworkPrefixLength()
              );
              for (String server : servers) {
                URL serverUrl = new URL(server);
                InetAddress eurekaServerAddress = InetAddress.getByName(serverUrl.getHost());
                boolean matches = subnet.getInfo().isInRange(eurekaServerAddress.getHostAddress());
                logger().info(loggerPrefix + "Testing server {} ({}): {}", server,
                    eurekaServerAddress.getHostAddress(), matches);
                if (matches) {
                  logger().info(loggerPrefix +
                          "Found Interface {}: {} ({})",
                      networkInterface.getName(),
                      interfaceAddress.getAddress().getHostName(),
                      interfaceAddress.getAddress().getHostAddress()
                  );
                  result = createEurekaInstanceConfigBean(inetUtils, instance, interfaceAddress);
                  break external_loop;
                }
              }
            } else {
              logger().info(loggerPrefix +
                      "Skipping IPv6 from Interface {}: {}/{}",
                  networkInterface.getName(),
                  interfaceAddress.getAddress(),
                  interfaceAddress.getNetworkPrefixLength()
              );
            }
          }
        }
      } catch (Exception e) {
        logger().error(loggerPrefix + "Error while detecting eureka client address", e);
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    return result;
  }

  private void setupJmxPort(EurekaInstanceConfigBean instance, Integer jmxPort) {
    Map<String, String> metadataMap = instance.getMetadataMap();
    if (metadataMap.get("jmx.port") == null && jmxPort != null) {
      metadataMap.put("jmx.port", String.valueOf(jmxPort));
    }
  }

  private EurekaInstanceConfigBean createEurekaInstanceConfigBean(InetUtils inetUtils,
      EurekaInstanceConfigBean defaultResult, InterfaceAddress interfaceAddress) {
    EurekaInstanceConfigBean result;
    result = new EurekaInstanceConfigBean(inetUtils);
    result.setPreferIpAddress(true);
    result.setHostname(interfaceAddress.getAddress().getHostName());
    result.setIpAddress(interfaceAddress.getAddress().getHostAddress());
    result.setSecurePortEnabled(defaultResult.isSecurePortEnabled());
    result.setSecurePort(defaultResult.getSecurePort());
    result.setNonSecurePortEnabled(defaultResult.isNonSecurePortEnabled());
    result.setNonSecurePort(defaultResult.getNonSecurePort());
    result.setMetadataMap(defaultResult.getMetadataMap());
    if (result.isSecurePortEnabled()) {
      result.setInstanceId(result.getIpAddress() + ":" + result.getHostname() + ":" +
          result.getSecurePort());
      result.setSecureHealthCheckUrl("https://" + result.getIpAddress() + ":" +
          result.getSecurePort() + defaultResult.getHealthCheckUrlPath());
    } else {
      result.setInstanceId(result.getIpAddress() + ":" + result.getHostname() + ":" +
          result.getNonSecurePort());
      result.setHealthCheckUrl("http://" + result.getIpAddress() + ":" +
          result.getNonSecurePort() + defaultResult.getHealthCheckUrlPath());
      result.setStatusPageUrl("http://" + result.getIpAddress() + ":" +
          result.getNonSecurePort() + defaultResult.getStatusPageUrlPath());
    }
    return result;
  }
}
