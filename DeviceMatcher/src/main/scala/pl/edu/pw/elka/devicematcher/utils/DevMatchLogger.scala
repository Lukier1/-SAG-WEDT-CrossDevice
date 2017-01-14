package pl.edu.pw.elka.devicematcher.utils

import akka.event.Logging.Debug
import org.apache.log4j

/**
  * Created by dawid on 11.01.17.
  *
  * Klasa pomocnicza do tworzenia loggera do piszącego do pliku i/lub stdout.
  */
object DevMatchLogger {
  val LOG_DIR = "./src/main/resources/logs/"
  val PRINT_FORMAT = "%d{yyyy-MM-dd HH:mm:ss} %-5p [%c{1}]: %m%n"

  /**
    * Metoda-skrot: tworzy loggera z domyslnym formatem i sciezka zapisu plikow logu.
    *
    * @param name nazwa loggera używana jako identyfikator
    * @param level obsługiwany poziom logowania log4j: ALL, DEBUG, ERROR, FATAL, INFO, TRACE, WARN, OFF
    * @param logToFile flaga logowania do pliku
    * @param filename nazwa pliku logu zapisywanego w LOG_DIR
    * @param logToStdout flaga logowania do stdout
    * @return stworzony logger lub null
    */
  def getLogger(name: String, level: log4j.Level, logToFile: Boolean, filename: String, logToStdout: Boolean): log4j.Logger = {
    if (name == null || name.isEmpty() || level == null || filename == null || filename.isEmpty())
      return null
    val logger = log4j.Logger.getLogger(name)
    logger.setLevel(level)
    logger.setAdditivity(false)
    if (logToFile)
      logger.addAppender(getFileAppender(name, level, LOG_DIR + filename))
    if (logToStdout)
      logger.addAppender(getStdoutAppender())
    logger
  }

  /**
    * Metoda-skrot: tworzy loggera z domyslnym formatem i sciezka zapisu plikow logu.
    *
    * @param name nazwa loggera używana jako identyfikator
    * @param filename nazwa pliku logu zapisywanego w LOG_DIR
    * @param level obsługiwany poziom logowania log4j: ALL, DEBUG, ERROR, FATAL, INFO, TRACE, WARN, OFF
    * @return stworzony logger lub null
    */
  def getLogger(name: String, filename : String = "DeviceMatherApp.log", level : log4j.Level = log4j.Level.DEBUG): log4j.Logger = {
    if (name == null || name.isEmpty() || filename == null || filename.isEmpty())
      return null
    val logger = log4j.Logger.getLogger(name)
    logger.setAdditivity(false)

    logger.addAppender(getFileAppender(name, level, LOG_DIR + filename ))

    logger.addAppender(getStdoutAppender())
    logger
  }

  /**
    * Tworzy loggera o zadanej nazwie i poziomie logowania.
    *
    * @param name nazwa loggera używana jako identyfikator
    * @param level obsługiwany poziom logowania log4j: ALL, DEBUG, ERROR, FATAL, INFO, TRACE, WARN, OFF
    * @return stworzony logger lub null
    */
  def getLogger(name: String, level: log4j.Level): log4j.Logger = {
    if (name == null || name.isEmpty() || level == null)
      return null
    val logger = log4j.Logger.getLogger(name)
    logger.setLevel(level)
    logger.setAdditivity(false)
    logger
  }

  /**
    * Tworzy appendera do pliku używanego przez loggera. Logger może mieć 0, 1 lub więcej różnych appenderów.
    *
    * @param name nazwa appendera używana jako identyfikator i wyśweitlana w logu (może to być np. nazwa klasy)
    * @param level obsługiwany poziom logowania log4j: ALL, DEBUG, ERROR, FATAL, INFO, TRACE, WARN, OFF
    * @param filepath ścieżka do pliku logowania
    * @param format format wpisów do loga
    * @param append flaga dopisywania: true - append, false - truncate
    * @return stworzony appender do pliku lub null
    */
  def getFileAppender(name: String, level: log4j.Level, filepath: String,
                      format: String = PRINT_FORMAT, append: Boolean = false): log4j.FileAppender = {
    if (name == null || name.isEmpty() || filepath == null || filepath.isEmpty() || format == null || format.isEmpty())
      return null
    val fa = new log4j.FileAppender()
    fa.setName(name)
    fa.setFile(filepath)
    fa.setLayout(new log4j.PatternLayout(format))
    fa.setThreshold(level)
    fa.setAppend(append)
    fa.activateOptions()
    fa
  }

  /**
    * Zwraca konsolowy appender 'STDOUT' zdefiniowany w resources/log4j.properties
    *
    * @return appender do stdout lub null
    */
  def getStdoutAppender(): log4j.Appender = {
    log4j.Logger.getRootLogger().getAppender("STDOUT")
  }
}