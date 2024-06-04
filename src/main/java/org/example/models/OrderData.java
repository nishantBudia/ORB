package org.example.models;

import java.util.Date;
import lombok.Data;
import org.example.enums.OrderSignal;
import org.example.enums.OrderType;

@Data
public class OrderData {
  private Date timestamp;
  private double price;
  private OrderType type;
  private OrderSignal signal;
}
