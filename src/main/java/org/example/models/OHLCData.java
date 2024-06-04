package org.example.models;

import java.util.Date;
import lombok.Data;

@Data
public class OHLCData {
  private Date timestamp;
  private double open;
  private double high;
  private double low;
  private double close;
}
