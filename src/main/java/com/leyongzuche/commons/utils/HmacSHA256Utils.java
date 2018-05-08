package com.leyongzuche.commons.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author pengqingsong
 * @desc HmacSHA256加密算法工具类
 */
@Slf4j
public final class HmacSHA256Utils {

    private static String digest(String secret, String content) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] secretByte = secret.getBytes("utf-8");
        byte[] dataBytes = content.getBytes("utf-8");
        SecretKey secretKey = new SecretKeySpec(secretByte, "HMACSHA256");
        mac.init(secretKey);
        byte[] doFinal = mac.doFinal(dataBytes);
        byte[] hexB = new Hex().encode(doFinal);
        return new String(hexB, "utf-8");
    }

    public static String digest(String secret, Map<String, String> map) {
        if (map.size() == 0) {
            throw new IllegalArgumentException("map中无值");
        }

        List<String> keys = new ArrayList(map.keySet());
        Collections.sort(keys);


        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < keys.size() - 1; i++) {
            String key = keys.get(i);
            String value = map.get(key);
            sb.append(key).append("=").append(value).append("&");
        }
        String lastKey = keys.get(keys.size() - 1);
        String lastValue = map.get(lastKey);
        sb.append(lastKey).append("=").append(lastValue);
        try {
            return digest(secret, sb.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
