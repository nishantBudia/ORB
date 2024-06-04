package org.example.exceptions;

public class CSVParsingException extends RuntimeException {
  public CSVParsingException(Throwable cause) {
    super(cause);
  }
}
