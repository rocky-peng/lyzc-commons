package com.leyongzuche.commons.login3.wechat;

import lombok.Data;

/**
 * @author pengqingsong
 * @date 26/09/2017
 * @desc
 */
@Data
public class AccessTokenDto {

    private String accessToken;

    private String openId;

    /**
     * 单位 秒
     */
    private int expiresIn;

    private String refreshToken;

    private String scope;
}
