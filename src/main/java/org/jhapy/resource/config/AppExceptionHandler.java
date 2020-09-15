package org.jhapy.resource.config;

import java.util.Optional;
import org.jhapy.commons.security.SecurityUtils;
import org.jhapy.commons.utils.HasLogger;
import org.jhapy.dto.serviceQuery.ServiceResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Alexandre Clavaud.
 * @version 1.0
 * @since 15/09/2020
 */
@RestControllerAdvice
public class AppExceptionHandler implements HasLogger {

  @ResponseBody
  @ExceptionHandler(value = AccessDeniedException.class)
  public ResponseEntity<?> handleException(AccessDeniedException exception) {
    Optional<String> user = SecurityUtils.getCurrentUserLogin();

    if (user.isPresent()) {
      logger().warn("User: " + user.get()
          + " attempted to access an unauthorized area : " + exception.getMessage(), exception);
    }

    return ResponseEntity.ok(new ServiceResult<>(exception));
  }
}
