package com.leyongzuche.commons.utils;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author pengqingsong
 * @date 07/09/2017
 */
public class PasswordUtils {

    public static String encrypt(String passwd) {
        return DigestUtils.md5Hex(passwd);
    }


}
