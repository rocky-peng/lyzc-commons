package com.leyongzuche.commons.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pengqingsong
 * 29/10/2017
 */
public class MiscUtils {

    public static String randomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
    }

    /**
     * 隐藏手机号中间4位
     */
    public static String hidePhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return "0";
        }

        Pattern p = Pattern.compile("\\d{4}(\\d{4})$");
        Matcher matcher = p.matcher(phone);
        if (matcher.find()) {
            StringBuffer sb = new StringBuffer(phone.length());
            matcher.appendReplacement(sb, "****" + matcher.group(1));
            matcher.appendTail(sb);
            return sb.toString();
        }
        return phone;
    }
}
