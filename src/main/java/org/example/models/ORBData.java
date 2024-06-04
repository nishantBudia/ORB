package org.example.models;

import java.util.Date;
import lombok.Data;

@Data
public class ORBData {
  private double upper;
  private double lower;
  private Date start;
  private Date end;
}
