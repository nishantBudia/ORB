package org.example;

import java.util.*;

import org.example.enums.CSV;
import org.example.models.OHLCData;
import org.example.models.TradeData;
import org.example.utils.DataUtils;
import org.example.utils.LogUtils;
import org.example.utils.StrategyUtils;

public class Main {
  private static final long startTime = new Date().getTime();

  public static void logTime() {
    String message = "Time Log :: " + ((new Date().getTime()) - startTime);
    LogUtils.info(message);
    System.out.println(message);
  }

  public static void main(String[] args) {
    try {
      logTime();
      Map<Date, List<OHLCData>> dayWiseOHLCData =
          DataUtils.getDayWiseOHLCData(DataUtils.readOHLCCsv(CSV.NIFTY_50_10YEAR_MINUTE_WISE));
      logTime();
      List<TradeData> tradeDataList = StrategyUtils.getListOfTradesForORBAdvanced(dayWiseOHLCData);
      tradeDataList.sort((oldTrade, newTrade)->{
        long timeDiff = newTrade.getEntry().getTimestamp().getTime()-oldTrade.getEntry().getTimestamp().getTime();
        return timeDiff>0?-1:1;
      });
      DataUtils.writeTradeDataToCSV(tradeDataList);
      logTime();
    } catch (Exception e) {
      LogUtils.error(Arrays.toString(e.getStackTrace()));
    } finally {
      LogUtils.dumpLogFile();
    }
  }
}
