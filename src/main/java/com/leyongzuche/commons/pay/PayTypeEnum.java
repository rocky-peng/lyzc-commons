package com.leyongzuche.commons.pay;

import lombok.Getter;

/**
 * @author pengqingsong
 * 12/09/2017
 */
public enum PayTypeEnum {


    //付款方式，1:线上支付宝，2:线下支付宝，3:线上微信，4:线下微信，5:线上银联，6:线下银联(pos机)，8:现金


    /**
     * 线上支付宝
     */
    ALIPAY_ONLINE(1),

    /**
     * 线下支付宝
     */
    ALIPAY_OFFLINE(2),

    /**
     * 线上微信
     */
    WEIXIN_ONLINE(3),

    /**
     * 线下微信
     */
    WEIXIN_OFFLINE(4),
    /**
     * 线上银联
     */
    UNIONPAY_ONLINE(5),

    /**
     * 线下银联(pos机)
     */
    UNIONPAY_OFFLINE(6),

    /**
     * 苹果支付
     */
    APPLE(7),
    /**
     * 现金
     */
    CASH(8);

    @Getter
    private int value;

    PayTypeEnum(int value) {
        this.value = value;
    }

}
