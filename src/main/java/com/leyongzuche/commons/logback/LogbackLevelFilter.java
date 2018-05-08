package com.leyongzuche.commons.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pengqingsong
 */
public class LogbackLevelFilter extends AbstractMatcherFilter<ILoggingEvent> {

    Map<String, Level> levels = new HashMap<>(5);

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (levels.containsKey(event.getLevel().levelStr)) {
            return onMatch;
        } else {
            return onMismatch;
        }
    }

    public void setLevel(Level level) {
        if (levels.containsKey(level.levelStr)) {
            return;
        }
        levels.put(level.levelStr, level);
    }

    @Override
    public void start() {
        if (levels.size() > 0) {
            super.start();
        } else {
            throw new RuntimeException("请设置要监控的日志级别,可设置多个");
        }
    }
}