package org.example.utils;

import static java.util.stream.Collectors.toList;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.example.enums.CSV;
import org.example.enums.OrderSignal;
import org.example.exceptions.CSVParsingException;
import org.example.models.OHLCData;
import org.example.models.OrderData;
import org.example.models.TradeData;

public class DataUtils {

  private static final String DATA_PATH = "data/";
  private static final double DEFAULT_CAPITAL = 0.0;
  private static final String OUTPUT_FOLDER = "output/";

  public static Map<Date, List<OHLCData>> getDayWiseOHLCData(List<OHLCData> ohlcDataList) {
    return ohlcDataList.stream()
        .collect(
            Collectors.groupingBy(
                (ohlcData -> {
                  Calendar cal = Calendar.getInstance();
                  cal.setTime(ohlcData.getTimestamp());
                  cal.set(Calendar.HOUR_OF_DAY, 0);
                  cal.set(Calendar.MINUTE, 0);
                  cal.set(Calendar.SECOND, 0);
                  cal.set(Calendar.MILLISECOND, 0);
                  return cal.getTime();
                }),
                toList()));
  }

  public static List<OHLCData> readOHLCCsv(CSV csvFile) {
    try {
      List<List<String>> records = getRecordsForCSV(csvFile);
      convertTimestampForFormatter(records, csvFile.getFormat());
      return convertRecordsToData(records);
    } catch (Exception e) {
      throw new CSVParsingException(e);
    }
  }

  private static List<OHLCData> convertRecordsToData(List<List<String>> records) {
    List<OHLCData> ohlcDataList = new ArrayList<>();
    for (List<String> record : records) {
      OHLCData ohlcData = new OHLCData();
      ohlcData.setTimestamp(new Date(Long.parseLong(record.get(0))));
      ohlcData.setOpen(Double.parseDouble(record.get(1)));
      ohlcData.setHigh(Double.parseDouble(record.get(2)));
      ohlcData.setLow(Double.parseDouble(record.get(3)));
      ohlcData.setClose(Double.parseDouble(record.get(4)));
      ohlcDataList.add(ohlcData);
    }
    return ohlcDataList;
  }

  public static void makeOHLCListConsistent(List<OHLCData> ohlcDataList) {
    for(int i = 1; i < ohlcDataList.size(); i++) {
      ohlcDataList.get(i).setOpen(ohlcDataList.get(i - 1).getClose());
    }
  }

  private static List<List<String>> getRecordsForCSV(CSV csvFile) {
    try {
      List<List<String>> records =
          Files.readAllLines(Paths.get(DATA_PATH + csvFile.getValue())).stream()
              .map(line -> Arrays.asList(line.split(",")))
              .toList();
      LogUtils.info("CSV PARSED :: records: " + records.getFirst() + " " + records.getLast());
      records = records.stream().skip(1).toList();
      return records;

    } catch (Exception e) {
      throw new CSVParsingException(e);
    }
  }

  private static void convertTimestampForFormatter(List<List<String>> records, String format) {
    LogUtils.info("Converting timestamp for format: " + format);
    if (Objects.isNull(format)) {
      return;
    }
    SimpleDateFormat formatter = new SimpleDateFormat(format);
    try {
      for (List<String> record : records) {
        record.set(0, String.valueOf(formatter.parse(record.get(0)).getTime()));
      }
    } catch (Exception e) {
      throw new CSVParsingException(e);
    }
  }

  public static void writeTradeDataToCSV(List<TradeData> tradeDataList) {
    try {
      double cumProfits = 0.0;
      StringBuilder csvBuilder = new StringBuilder();
      csvBuilder.append("Trade #,Type,Signal,Timestamp,Price INR,Profit INR,Profit %,Cum. Profit INR,Cum. Profit %\n");
      int successTimes = 0;
      for (int i = 0; i < tradeDataList.size(); i++) {
        csvBuilder.append(getCSVRowForTradeData(i + 1, tradeDataList.get(i), cumProfits));
        cumProfits += tradeDataList.get(i).getProfit();
        if(OrderSignal.BP.equals(tradeDataList.get(i).getExit().getSignal())) {
          successTimes++;
        }
      }
      LogUtils.info("Strategy Success times :: " + successTimes);
      LogUtils.info("Strategy total trades :: " + tradeDataList.size());
      String fileName = OUTPUT_FOLDER + "Trade-Output " + new Date() + ".csv";
      PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8);
      writer.println(csvBuilder);
      writer.close();
    } catch (Exception e) {
      throw new CSVParsingException(e);
    }
  }

  private static String getCSVRowForTradeData(
      int tradeNumber, TradeData tradeData, double cumProfits) {
    double cumProfitPercent =
        (Math.abs(tradeData.getProfit()) / (DEFAULT_CAPITAL + Math.abs(cumProfits))) * 100;
    cumProfits += tradeData.getProfit();
    OrderData entryData = tradeData.getEntry();
    String entryRow =
        tradeNumber
            + ","
            + entryData.getType()
            + ","
            + entryData.getSignal()
            + ","
            + entryData.getTimestamp()
            + ","
            + entryData.getPrice()
            + ","
            + tradeData.getProfit()
            + ","
            + tradeData.getProfitPercent()
            + ","
            + cumProfits
            + ","
            + cumProfitPercent
            + "\n";
    OrderData exitData = tradeData.getExit();
    String exitRow =
        tradeNumber
            + ","
            + exitData.getType()
            + ","
            + exitData.getSignal()
            + ","
            + exitData.getTimestamp()
            + ","
            + exitData.getPrice()
            + ","
            + tradeData.getProfit()
            + ","
            + tradeData.getProfitPercent()
            + ","
            + cumProfits
            + ","
            + cumProfitPercent
            + "\n";
    return entryRow + exitRow;
  }
}
