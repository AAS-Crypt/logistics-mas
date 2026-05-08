package com.logistics.util;

import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;


public class Logger {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("LogisticsMAS");
    
    static {
        
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.INFO);
        
        LOGGER.setUseParentHandlers(false);
    }

     
    public static void info(String source, String message) {
        LOGGER.log(Level.INFO, "[" + source + "] " + message);
    }

     
    public static void warning(String source, String message) {
        LOGGER.log(Level.WARNING, "[" + source + "] " + message);
    }

     
    public static void error(String source, String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, "[" + source + "] " + message, throwable);
    }

     
    public static void error(String source, String message) {
        LOGGER.log(Level.SEVERE, "[" + source + "] " + message);
    }

     
    public static void debug(String source, String message) {
        LOGGER.log(Level.FINE, "[" + source + "] " + message);
    }

     
    public static void setLevel(Level level) {
        LOGGER.setLevel(level);
    }
}