package com.leyongzuche.commons.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * 时间相关的工具类
 *
 * @author pengqingsong
 * 09/09/2017
 */
public class DateTimeUtils {


    public static long endOfDay(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime().getTime();
    }

    public static boolean isEndOfDay(long mills) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(mills));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        return hour == 23 && minute == 59 && second == 59 && millisecond == 999;
    }

    public static long startOfDay(Date day) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }

    public static boolean isStartOfDay(long mills) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(mills));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        return hour == 0 && minute == 0 && second == 0 && millisecond == 0;
    }

    /**
     * 计算从此时到今天结束还剩多长时间，单位毫秒
     */
    public static long nowToEndOfToday() {
        Date today = new Date();
        return endOfDay(today) - today.getTime();
    }


    /**
     * 判断给定的时间戳是否是 0分0秒0毫秒
     */
    public static boolean isStartOfHour(long mills) {
        Date date = new Date(mills);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        return minute == 0 && second == 0 && millisecond == 0;
    }

    public static long endOfHour(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(millis));
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime().getTime();
    }

    /**
     * 判断给定的时间戳是否是 59分59秒999毫秒
     */
    public static boolean isEndOfHour(long mills) {
        Date date = new Date(mills);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        return minute == 59 && second == 59 && millisecond == 999;
    }

    /**
     * 获取给定时间戳的小时数
     */
    public static int getHour(long mills) {
        Date date = new Date(mills);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }
}
