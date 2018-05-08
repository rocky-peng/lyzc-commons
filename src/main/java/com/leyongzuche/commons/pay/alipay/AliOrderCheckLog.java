package com.leyongzuche.commons.pay.alipay;

import com.leyongzuche.commons.utils.JsonUtils;
import lombok.Data;

import java.util.Map;

/**
 * @author pengqingsong
 * @date 12/09/2017
 * @desc
 */
@Data
final class AliOrderCheckLog {
    /**
     * 验签是否通过
     */
    private boolean verified;

    /**
     * 支付宝传递过来的所有参数
     */
    private Map aliParams;

    /**
     * 对验证结果的描述信息
     */
    private String msg;

    /**
     * 相关的订单id
     */
    private long orderId;

    /**
     * 返回给支付宝的信息
     */
    private String result = "failure";

    @Override
    public String toString() {
        return JsonUtils.serialize(this);
    }
}
