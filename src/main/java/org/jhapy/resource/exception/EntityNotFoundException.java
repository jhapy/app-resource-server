package org.jhapy.resource.exception;

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-04-14
 */
public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException() {
  }

  public EntityNotFoundException(String message) {
    super(message);
  }
}
