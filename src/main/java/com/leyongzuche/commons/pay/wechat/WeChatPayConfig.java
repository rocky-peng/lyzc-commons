package com.leyongzuche.commons.pay.wechat;

import lombok.Data;

@Data
public class WeChatPayConfig {

    /**
     * App ID
     */
    private String appId;

    /**
     * Mch ID
     */
    private String mchID;


    /**
     * API 密钥
     */
    private String key;


    /**
     * 商户证书路径（相对于classpath根路径，所以必须以 / 开头）
     */
    private String pkcs12FilePath;

    /**
     * 微信支付RSA算法公钥 相对于classpath根路径，所以必须以 / 开头）
     */
    private String pkcs8FilePath;

    /**
     * 商户证书路径密码
     */
    private String pkcs12FilePasswd;

    /**
     * HTTP(S) 连接超时时间，单位毫秒
     */
    private int connectTimeoutMs;

    /**
     * HTTP(S) 读数据超时时间，单位毫秒
     */
    private int httpReadTimeoutMs;

    private WeChatPaySignTypeEnum signType;

    private String notifyUrl;
}
