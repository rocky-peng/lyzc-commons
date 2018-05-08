package com.leyongzuche.commons.pay.wechat;

import lombok.Getter;

/**
 * @author pengqingsong
 * @date 02/02/2018
 * @desc
 */
public enum TransferToBankResultEnum {

// 0，刚提交尚未提交到第三方支付系统
// 1：已经提交到第三方系统，但还在处理中
// 2：第三方系统提现完成（转账成功）
// 3：提现失败
// 4：银行退票

    IN_DB(0, "已受理"),
    PROCESSING(1, "第三方支付系统处理中"),
    SUCCESS(2, "付款成功"),
    FAILED(3, "付款失败"),
    BANK_FAIL(4, "银行退票");

    @Getter
    private String desc;

    @Getter
    private int status;

    TransferToBankResultEnum(int status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}
