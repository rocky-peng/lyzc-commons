package com.leyongzuche.commons.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 一些公共的正则表达式
 *
 * @author pengqingsong
 * 09/09/2017
 */
public class PatternUtils {

    public static final Pattern PHONE_PATTERN = Pattern.compile("^1[0-9]{10}$");
    public static final Pattern CAPTCHA_4_PATTERN = Pattern.compile("^[0-9]{4}$");
    public static final Pattern CAPTCHA_6_PATTERN = Pattern.compile("^[0-9]{6}$");
    public static final Pattern CAR_NUM_PATTERN = Pattern.compile("^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领A-Z]{1}[A-Z]{1}[A-Z0-9]{4}[A-Z0-9挂学警港澳]{1}$");


    public static boolean isValidPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).find();
    }

    public static boolean isValid6Captcha(String captcha) {
        if (StringUtils.isBlank(captcha)) {
            return false;
        }
        return CAPTCHA_6_PATTERN.matcher(captcha).find();
    }

    public static boolean isValidCarNum(String carNum) {
        if (StringUtils.isBlank(carNum)) {
            return false;
        }
        return CAR_NUM_PATTERN.matcher(carNum).find();
    }

}
