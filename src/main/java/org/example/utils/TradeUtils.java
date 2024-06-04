package org.example.utils;

import static org.example.constants.TradeExceptionConstants.TRADE_INCOMPLETE;

import java.util.Date;
import org.example.enums.OrderSignal;
import org.example.enums.OrderType;
import org.example.enums.TradeStatus;
import org.example.exceptions.TradeException;
import org.example.models.OHLCData;
import org.example.models.OrderData;
import org.example.models.TradeData;

public class TradeUtils {

  private static final double RISK_FACTOR_DEFAULT = 2;

  public static OrderData createOrder(
      Date timestamp, double price, OrderType type, OrderSignal signal) {
    OrderData orderData = new OrderData();
    orderData.setTimestamp(timestamp);
    orderData.setPrice(price);
    orderData.setType(type);
    orderData.setSignal(signal);
    return orderData;
  }

  public static TradeData enterTrade(OHLCData ohlcData, OrderType type, OrderSignal signal) {
    TradeData tradeData = new TradeData();
    tradeData.setEntry(createOrder(ohlcData.getTimestamp(), ohlcData.getClose(), type, signal));
    putBPAndSL(tradeData, ohlcData, type);
    tradeData.setStatus(TradeStatus.ACTIVE);
    return tradeData;
  }

  public static void putBPAndSL(TradeData tradeData, OHLCData ohlcData, OrderType orderType) {
    double risk = getRiskForOrderType(ohlcData, orderType);
    tradeData.setBookProfits(getBPForOrderType(ohlcData, orderType, risk));
    tradeData.setStopLoss(getSLForOrderType(ohlcData, orderType, risk));
  }

  private static double getSLForOrderType(OHLCData ohlcData, OrderType orderType, double risk) {
    return switch (orderType) {
      case SHORT -> ohlcData.getClose() + risk;
      case LONG -> ohlcData.getClose() - risk;
      default -> 0.0;
    };
  }

  public static double getBPForOrderType(OHLCData ohlcData, OrderType orderType, double risk) {
    return switch (orderType) {
      case SHORT -> ohlcData.getClose() - (RISK_FACTOR_DEFAULT * risk);
      case LONG -> ohlcData.getClose() + (RISK_FACTOR_DEFAULT * risk);
      default -> 0.0;
    };
  }

  public static double getRiskForOrderType(OHLCData ohlcData, OrderType orderType) {
    return switch (orderType) {
      case SHORT -> ohlcData.getHigh() - ohlcData.getClose();
      case LONG -> ohlcData.getClose() - ohlcData.getLow();
      default -> 0.0;
    };
  }

  public static void exitTrade(OHLCData ohlcData, OrderSignal signal, TradeData tradeData) {
    tradeData.setExit(
        createOrder(
            ohlcData.getTimestamp(),
            ohlcData.getClose(),
            tradeData.getEntry().getType().getNext(),
            signal));
    tradeData.setStatus(TradeStatus.COMPLETED);
    calculateTradeProfits(tradeData);
  }

  public static void calculateTradeProfits(TradeData tradeData) {
    if (isShortTrade(tradeData)) {
      calculateShortTradeProfits(tradeData);
    } else if (isLongTrade(tradeData)) {
      calculateLongTradeProfits(tradeData);
    } else {
      throw new TradeException(TRADE_INCOMPLETE);
    }
  }

  private static void calculateShortTradeProfits(TradeData tradeData) {
    double entryPrice = tradeData.getEntry().getPrice();
    double exitPrice = tradeData.getExit().getPrice();
    double profit = entryPrice - exitPrice;
    double profitPercentage = (profit / entryPrice) * 100;
    tradeData.setProfit(profit);
    tradeData.setProfitPercent(profitPercentage);
  }

  private static void calculateLongTradeProfits(TradeData tradeData) {
    double entryPrice = tradeData.getEntry().getPrice();
    double exitPrice = tradeData.getExit().getPrice();
    double profit = exitPrice - entryPrice;
    double profitPercentage = (profit / entryPrice) * 100;
    tradeData.setProfit(profit);
    tradeData.setProfitPercent(profitPercentage);
  }

  private static boolean isLongTrade(TradeData tradeData) {
    return isTradeCompleted(tradeData)
        && OrderType.LONG.equals(tradeData.getEntry().getType())
        && OrderType.EXIT_LONG.equals(tradeData.getExit().getType());
  }

  private static boolean isShortTrade(TradeData tradeData) {
    return isTradeCompleted(tradeData)
        && OrderType.SHORT.equals(tradeData.getEntry().getType())
        && OrderType.EXIT_SHORT.equals(tradeData.getExit().getType());
  }

  private static boolean isTradeCompleted(TradeData tradeData) {
    return TradeStatus.COMPLETED.equals(tradeData.getStatus());
  }

  public static boolean isTradeActive(TradeData tradeData) {
    return TradeStatus.ACTIVE.equals(tradeData.getStatus());
  }

  public static boolean isStopLoss(TradeData tradeData, OHLCData ohlcData) {
    return StrategyUtils.isValueInBetween(tradeData.getStopLoss(), ohlcData);
  }

  public static boolean isBookProfits(TradeData tradeData, OHLCData ohlcData) {
    return StrategyUtils.isValueInBetween(tradeData.getBookProfits(), ohlcData);
  }
}
