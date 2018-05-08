package com.leyongzuche.commons.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pengqingsong
 */
public enum CaptchaType {
    /**
     * 用于更新密码的验证码
     */
    FOR_UPDATE_PASSWD(1),

    /**
     * 用于绑定手机号的验证码
     */
    FOR_BIND_PHONE(2),

    /**
     * 用于验证码登录
     */
    FOR_LOGIN(3);

    private static final Map<Integer, CaptchaType> MAP = new HashMap<>();

    static {
        for (CaptchaType captchaType : CaptchaType.values()) {
            MAP.put(captchaType.getTypeId(), captchaType);
        }
    }

    @Getter
    private int typeId;

    CaptchaType(int typeId) {
        this.typeId = typeId;
    }

    public static CaptchaType getByTypeId(int typeId) {
        return MAP.get(typeId);
    }

}