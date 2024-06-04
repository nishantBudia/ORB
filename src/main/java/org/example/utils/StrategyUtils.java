package org.example.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.example.enums.OrderSignal;
import org.example.enums.OrderType;
import org.example.models.OHLCData;
import org.example.models.ORBAdvancedStrategyData;
import org.example.models.ORBData;
import org.example.models.TradeData;

public class StrategyUtils {
  private static final int DEFAULT_STRATEGY_MINUTES_MARGIN = 5;

  public static boolean crossOver(OHLCData ohlcData, double value) {
    return Objects.nonNull(ohlcData) && ohlcData.getOpen() < value && ohlcData.getClose() > value;
  }

  public static boolean crossUnder(OHLCData ohlcData, double value) {
    return Objects.nonNull(ohlcData) && ohlcData.getOpen() > value && ohlcData.getClose() < value;
  }

  public static boolean closedOver(OHLCData ohlcData, double value) {
    return Objects.nonNull(ohlcData) && ohlcData.getClose() >= value;
  }

  public static boolean closedUnder(OHLCData ohlcData, double value) {
    return Objects.nonNull(ohlcData) && ohlcData.getClose() <= value;
  }

  public static boolean isOver(OHLCData ohlcData, double value) {
    return Objects.nonNull(ohlcData) && ohlcData.getLow() > value;
  }

  public static boolean isUnder(OHLCData ohlcData, double value) {
    return Objects.nonNull(ohlcData) && ohlcData.getHigh() < value;
  }

  public static boolean whiteCandle(OHLCData ohlcData) {
    return Objects.nonNull(ohlcData) && ohlcData.getOpen() < ohlcData.getClose();
  }

  public static boolean blackCandle(OHLCData ohlcData) {
    return Objects.nonNull(ohlcData) && ohlcData.getOpen() > ohlcData.getClose();
  }

  public static boolean isValueInBetween(double value, OHLCData ohlcData) {
    return Objects.nonNull(ohlcData) && ohlcData.getHigh() > value && ohlcData.getLow() < value;
  }

  public static List<TradeData> getListOfTradesForORBAdvanced(
      Map<Date, List<OHLCData>> dayWiseOHLCData) {
    Map<Date, ORBData> dayWiseORBData = IndicatorUtils.getDayWiseORBData(dayWiseOHLCData);
    List<TradeData> tradeDataList = new ArrayList<>();
    for (Map.Entry<Date, List<OHLCData>> entry : dayWiseOHLCData.entrySet()) {
      tradeDataList.addAll(
          getListOfTradesForORBAdvancedForDay(
              entry.getValue(), dayWiseORBData.get(entry.getKey())));
    }
//    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//      try {
//          Date date = dateFormat.parse("2015-01-09 00:00:00");
//          tradeDataList.addAll(getListOfTradesForORBAdvancedForDay(dayWiseOHLCData.get(date), dayWiseORBData.get(date)));
//      } catch (ParseException e) {
//          throw new RuntimeException(e);
//      }
      return tradeDataList;
  }

  public static List<TradeData> getListOfTradesForORBAdvancedForDay(
      List<OHLCData> ohlcDataList, ORBData orbData) {
    //DataUtils.makeOHLCListConsistent(ohlcDataList);
    ORBAdvancedStrategyData strategyData = new ORBAdvancedStrategyData();
    AtomicReference<TradeData> tradeDataReference = new AtomicReference<>(new TradeData());
    List<TradeData> tradeDataList = new ArrayList<>();
    ohlcDataList.stream()
        .skip(
            ohlcDataList.stream().map(OHLCData::getTimestamp).toList().indexOf(orbData.getEnd())
                + 1)
        .limit(ohlcDataList.size() - DEFAULT_STRATEGY_MINUTES_MARGIN)
        .forEach(
            ohlcData -> {
              processSingleTradeDataPoint(orbData, ohlcData, tradeDataReference, tradeDataList, strategyData);
            });
    if (TradeUtils.isTradeActive(tradeDataReference.get())) {
      exitTradeAccordingToSignal(
          ohlcDataList.get(ohlcDataList.size() - DEFAULT_STRATEGY_MINUTES_MARGIN),
          OrderSignal.EOD,
          tradeDataReference,
          tradeDataList);
    }
    return tradeDataList;
  }

  private static void processSingleTradeDataPoint(ORBData orbData, OHLCData ohlcData, AtomicReference<TradeData> tradeDataReference, List<TradeData> tradeDataList, ORBAdvancedStrategyData strategyData) {
    if (TradeUtils.isTradeActive(tradeDataReference.get())) {
      if (TradeUtils.isBookProfits(tradeDataReference.get(), ohlcData)) {
        exitTradeAccordingToSignal(
                ohlcData, OrderSignal.BP, tradeDataReference, tradeDataList);
      } else if (TradeUtils.isStopLoss(tradeDataReference.get(), ohlcData)) {
        exitTradeAccordingToSignal(
                ohlcData, OrderSignal.SL, tradeDataReference, tradeDataList);
      }
    } else {
      if (isOver(ohlcData, orbData.getUpper())) {
        if (Objects.isNull(strategyData.getBreakoutValue()) && blackCandle(ohlcData)) {
          strategyData.setBreakoutValue(ohlcData.getLow());
        } else if (Objects.nonNull(strategyData.getBreakoutValue())
            && closedUnder(ohlcData, strategyData.getBreakoutValue())) {
          tradeDataReference.set(
              TradeUtils.enterTrade(ohlcData, OrderType.SHORT, OrderSignal.SHORT));
          strategyData.setBreakoutValue(null);
        }
      } else if (isUnder(ohlcData, orbData.getLower())) {
        if (Objects.isNull(strategyData.getBreakoutValue()) && whiteCandle(ohlcData)) {
          strategyData.setBreakoutValue(ohlcData.getHigh());
        } else if (Objects.nonNull(strategyData.getBreakoutValue())
            && closedOver(ohlcData, strategyData.getBreakoutValue())) {
          tradeDataReference.set(
              TradeUtils.enterTrade(ohlcData, OrderType.LONG, OrderSignal.LONG));
          strategyData.setBreakoutValue(null);
        }
      } else {
        strategyData.setBreakoutValue(null);
      }
    }
    LogUtils.info(ohlcData + " " + strategyData + " " + tradeDataReference.get());
  }

  private static void exitTradeAccordingToSignal(
      OHLCData ohlcData,
      OrderSignal bp,
      AtomicReference<TradeData> tradeDataReference,
      List<TradeData> tradeDataList) {
    TradeUtils.exitTrade(ohlcData, bp, tradeDataReference.get());
    tradeDataList.add(tradeDataReference.get());
  }
}
