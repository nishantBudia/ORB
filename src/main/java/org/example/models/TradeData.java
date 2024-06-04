package org.example.models;

import lombok.Data;
import org.example.enums.TradeStatus;

@Data
public class TradeData {
  private OrderData entry;
  private OrderData exit;
  private double bookProfits;
  private double stopLoss;
  private double profit;
  private double profitPercent;
  private TradeStatus status = TradeStatus.WAITING;
}
