package org.jhapy.resource.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.cloud.netflix.hystrix.HystrixStreamEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableWebSecurity
public class ActuatorSecurity {

  private Environment env;

  public ActuatorSecurity(Environment env) {
    this.env = env;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService actuatorUserDetailsService() throws Exception {
    UserBuilder users = User.builder().passwordEncoder(s -> passwordEncoder().encode(s));
    InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    manager.createUser(users.username(env.getProperty("spring.security.user.name")).password(
        env.getProperty("spring.security.user.password"))
        .roles(env.getProperty("spring.security.user.roles")).build());
    return manager;
  }

  @Configuration
  @Order(1)
  public static class MonitoringWebSecurityConfigurationAdapter extends
      WebSecurityConfigurerAdapter {

    private final UserDetailsService v3UserDetailsService;

    public MonitoringWebSecurityConfigurationAdapter(
        @Qualifier("actuatorUserDetailsService") UserDetailsService v3UserDetailsService) {
      this.v3UserDetailsService = v3UserDetailsService;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth
          .userDetailsService(v3UserDetailsService);
    }

    protected void configure(HttpSecurity http) throws Exception {
      http.antMatcher("/actuator/**")
          .authorizeRequests(authorize ->
              authorize.requestMatchers(EndpointRequest.to(HealthEndpoint.class),EndpointRequest.to(
                  HystrixStreamEndpoint.class)).permitAll()
                  .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("MONITORING")
                  .anyRequest().permitAll()
          ).httpBasic(Customizer.withDefaults());
    }
  }
}