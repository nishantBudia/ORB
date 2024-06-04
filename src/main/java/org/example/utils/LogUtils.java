package org.example.utils;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class LogUtils {
  private static final StringBuilder log = new StringBuilder();
  private static final String LOG_FOLDER = "logs/";

  public static void info(String inputLog) {
    log.append("[INFO] :: Timestamp: ")
        .append(new Date().toInstant())
        .append(" :: ")
        .append(inputLog)
        .append("\n");
  }

  public static void error(String inputLog) {
    log.append("[ERROR] :: Timestamp: ")
        .append(new Date().toInstant())
        .append(" :: ")
        .append(inputLog)
        .append("\n");
  }

  public static void dumpLogFile() {
    try {
      String fileName = LOG_FOLDER + "LOGFILE " + new Date() + ".txt";
      PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8);
      writer.println(log);
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Cannot print the logfile");
    }
  }
}
