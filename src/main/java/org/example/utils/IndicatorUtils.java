package org.example.utils;

import java.util.*;
import org.example.models.OHLCData;
import org.example.models.ORBData;

public class IndicatorUtils {
  private static final int DEFAULT_START_HOUR = 9;
  private static final int DEFAULT_START_MINUTE = 15;
  private static final int DEFAULT_END_HOUR = 9;
  private static final int DEFAULT_END_MINUTE = 29;

  public static Map<Date, ORBData> getDayWiseORBData(Map<Date, List<OHLCData>> dayWiseOHLCData) {
    Map<Date, ORBData> orbDataMap = new HashMap<>();
    for (Map.Entry<Date, List<OHLCData>> entry : dayWiseOHLCData.entrySet()) {
      ORBData orbData = getORBDataForDay(entry.getValue());
      orbDataMap.put(entry.getKey(), orbData);
    }
    return orbDataMap;
  }

  public static ORBData getORBDataForDay(List<OHLCData> dayOHLCData) {
    return getORBDataForDay(
        dayOHLCData,
        DEFAULT_START_HOUR,
        DEFAULT_START_MINUTE,
        DEFAULT_END_HOUR,
        DEFAULT_END_MINUTE);
  }

  /**
   * returns the ORB (high and low) for given list of OHLC, in between the specified start and end
   * timestamps
   *
   * @param dayOHLCData
   * @param startHour
   * @param startMinute
   * @param endHour
   * @param endMinute
   * @return
   */
  public static ORBData getORBDataForDay(
      List<OHLCData> dayOHLCData, int startHour, int startMinute, int endHour, int endMinute) {
    ORBData orbData = new ORBData();
    Date startDate = getDateForHourAndMinute(dayOHLCData, startHour, startMinute);
    Date endDate = getDateForHourAndMinute(dayOHLCData, endHour, endMinute);
    int lastIndex = dayOHLCData.size() - 1;
    int startIndex =
        Math.max(dayOHLCData.stream().map(OHLCData::getTimestamp).toList().indexOf(startDate), 0);
    int endIndex =
        Math.min(
            dayOHLCData.stream().map(OHLCData::getTimestamp).toList().indexOf(endDate), lastIndex);
    double upper = Double.MIN_VALUE;
    double lower = Double.MAX_VALUE;
    for (int i = startIndex; i <= endIndex; i++) {
      upper = Math.max(dayOHLCData.get(i).getHigh(), upper);
      lower = Math.min(dayOHLCData.get(i).getLow(), lower);
    }
    orbData.setStart(dayOHLCData.get(startIndex).getTimestamp());
    orbData.setEnd(dayOHLCData.get(endIndex).getTimestamp());
    orbData.setUpper(upper);
    orbData.setLower(lower);
    LogUtils.info("ORB Calculated: " + orbData);
    return orbData;
  }

  private static Date getDateForHourAndMinute(List<OHLCData> dayOHLCData, int hour, int minute) {
    Calendar cal = Calendar.getInstance();
    return dayOHLCData.stream()
        .map(OHLCData::getTimestamp)
        .filter(
            date -> {
              cal.setTime(date);
              return cal.get(Calendar.HOUR_OF_DAY) == hour && cal.get(Calendar.MINUTE) == minute;
            })
        .findFirst()
        .orElse(dayOHLCData.getFirst().getTimestamp());
  }
}
