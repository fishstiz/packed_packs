package io.github.fishstiz.fidgetz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
    public static final Logger LOGGER = LoggerFactory.getLogger("fidgetz");

    private LogUtil() {
    }

    public static void logUnsupported(String message) {
        if (!message.isEmpty()) {
            LOGGER.error("Unsupported operation: {}", message, new UnsupportedOperationException(message));
        } else {
            LOGGER.error("Unsupported operation.", new UnsupportedOperationException(message));
        }
    }

    public static void logUnsupported() {
        logUnsupported("");
    }
}
