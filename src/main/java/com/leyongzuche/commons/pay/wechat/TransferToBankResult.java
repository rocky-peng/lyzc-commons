package com.leyongzuche.commons.pay.wechat;

import lombok.Data;
import lombok.ToString;

/**
 * @author pengqingsong
 * @date 02/02/2018
 * @desc
 */
@Data
@ToString
public class TransferToBankResult {
    private TransferToBankResultEnum result;

    private String reason;

    private String paySuccessTime;

    public TransferToBankResult(TransferToBankResultEnum result, String reason, String paySuccessTime) {
        this.result = result;
        this.reason = reason;
        this.paySuccessTime = paySuccessTime;
    }
}
