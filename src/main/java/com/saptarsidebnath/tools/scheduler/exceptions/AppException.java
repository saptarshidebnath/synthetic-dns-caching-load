package com.saptarsidebnath.tools.scheduler.exceptions;

public class AppException extends RuntimeException {
  private final String message;

  public AppException(String message) {
    super(message);
    this.message = message;
  }
}
