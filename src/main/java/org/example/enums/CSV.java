package org.example.enums;

import lombok.Getter;

@Getter
public enum CSV {
  NIFTY_50_10YEAR_MINUTE_WISE("NIFTY 50 - Minute data.csv", "yyyy-MM-dd HH:mm:ss");

  private final String value;
  private final String format;

  CSV(String value, String format) {
    this.value = value;
    this.format = format;
  }
}
