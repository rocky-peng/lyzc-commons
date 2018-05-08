package com.leyongzuche.commons.pay.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.leyongzuche.commons.pay.PayBusinessCallback;
import com.leyongzuche.commons.pay.PayTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.Map;

/**
 * @author pengqingsong
 * @date 12/09/2017
 * @desc
 */
@Slf4j
public final class Alipay {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";
    private String format = "json";
    private String charset = "UTF-8";
    private String signType = "RSA2";

    private volatile AlipayClient alipayClient;

    private String appId;
    private String appPrivateKey;
    private String appPublicKey;
    private String alipayPublicKey;
    private String alipayGateway;
    private String callbackUrl;

    public Alipay(String appId, String appPrivateKey, String appPublicKey, String alipayPublicKey, String alipayGateway, String callbackUrl) {
        this.appId = appId;
        this.appPrivateKey = appPrivateKey;
        this.appPublicKey = appPublicKey;
        this.alipayPublicKey = alipayPublicKey;
        this.alipayGateway = alipayGateway;
        this.callbackUrl = callbackUrl;
    }

    /**
     * 生成支付宝的签名信息，用于前端调用支付宝sdk
     *
     * @param orderCode   订单的编号，全局唯一（本地，测试，线上等各个环境都唯一）
     * @param orderName   订单名称，会显示给终端用户
     * @param orderPrice  订单金额，精确到分
     * @param expiredTime 返回的签名信息过期时间，超过这个时间后签名的信息不可用，单位为 分钟。范围[1,21600]，就是1分钟到15天
     * @return 用于支付宝支付的签名信息
     */
    public String getAliSignedOrderInfo(String orderCode, String orderName, double orderPrice, int expiredTime) {
        if (expiredTime < 1 || expiredTime > 21600) {
            throw new IllegalArgumentException("expiredTime");
        }
        AlipayClient alipayClient = getAlipayClient();
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setOutTradeNo(orderCode);
        model.setBody(orderCode);
        model.setSubject(orderName);
        model.setProductCode("QUICK_MSECURITY_PAY");
        model.setTimeoutExpress(expiredTime + "m");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        model.setTotalAmount(nf.format(orderPrice));
        request.setBizModel(model);
        request.setNotifyUrl(callbackUrl);
        String body = null;
        try {
            body = alipayClient.sdkExecute(request).getBody();
        } catch (Exception e) {
            throw new RuntimeException("后台调用支付宝sdk对订单信息签名失败！", e);
        }
        return body;
    }

    /**
     * 当订单状态发生变化时，进行一些验证工作。
     *
     * @param params              支付宝传递过来的所有参数，可以不用排序
     * @param payBusinessCallback 和业务相关的处理代码
     * @return 验证成功，返回 success 否则 failure  可以直接将该结果返回给支付宝
     */
    public String alipayCallback(Map<String, String> params, PayBusinessCallback payBusinessCallback) {
        AliOrderCheckLog checkLog = new AliOrderCheckLog();
        checkLog.setAliParams(params);
        boolean verified = false;
        AlipayApiException ex = null;
        try {
            verified = AlipaySignature.rsaCheckV1(params, alipayPublicKey, charset, signType);
        } catch (AlipayApiException e) {
            ex = e;
        }

        if (!verified) {
            checkLog.setVerified(false);
            checkLog.setMsg("验签不通过" + (ex != null ? "[" + ex.getErrMsg() + "]" : ""));
            log.info(checkLog.toString());
            return FAILURE;
        }
        checkLog.setVerified(true);

        //验证out_trade_no，该参数必须有
        String out_trade_no = params.get("out_trade_no");
        if (StringUtils.isBlank(out_trade_no)) {
            checkLog.setMsg("out_trade_no无数值");
            log.info(checkLog.toString());
            return FAILURE;
        }

        String appId = params.get("app_id");
        if (!this.appId.equals(appId)) {
            checkLog.setMsg("app_id无数值或者是无效值");
            log.info(checkLog.toString());
            return FAILURE;
        }

        String trade_status = params.get("trade_status");
        //交易创建，等待买家付款，按照支付宝文档是不会存在这种状态
        if ("WAIT_BUYER_PAY".equals(trade_status)) {
            //do nothing
            checkLog.setMsg(SUCCESS);
            log.info(checkLog.toString());
            return SUCCESS;
        }

        String orderCode = out_trade_no;
        long orderId = payBusinessCallback.getOrderIdByOrderCode(orderCode);
        if (orderId <= 0) {
            checkLog.setMsg("out_trade_no是无效值");
            log.info(checkLog.toString());
            return FAILURE;
        }
        checkLog.setOrderId(orderId);

        //验证 total_amount ，这个参数非必须，不清楚什么情况有什么情况下无，所以暂时不验证
        /*if (params.get("total_amount") == null) {
            checkLog.setMsg("total_amount无数值");
            log.info(checkLog.toString());
            return false;
        }
        BigDecimal totalAmount = new BigDecimal(params.get("total_amount"));
        if (totalAmount.compareTo(orderInfo.getTotalPrice()) != 0) {
            checkLog.setMsg("total_amount和数据库中的数据不一致");
            log.info(checkLog.toString());
            return false;
        }*/

        //支付宝文档说要验证seller_id,我们没有相关信息

        if ("TRADE_CLOSED".equals(trade_status)) {
            if (payBusinessCallback.doWhenTradeClosed(orderCode, PayTypeEnum.ALIPAY_ONLINE)) {
                log.info(checkLog.toString());
                checkLog.setMsg(SUCCESS);
                return SUCCESS;
            }
            checkLog.setMsg("doWhenTradeClosed 方法返回false,orderId=" + orderId);
            log.info(checkLog.toString());
            return FAILURE;
        }

        if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
            if (payBusinessCallback.doWhenTradeSuccess(orderCode, PayTypeEnum.ALIPAY_ONLINE, null)) {
                checkLog.setMsg(SUCCESS);
                log.info(checkLog.toString());
                return SUCCESS;
            }
            checkLog.setMsg("doWhenTradeSuccess 方法返回false,orderCode=" + orderCode);
            log.info(checkLog.toString());
            return FAILURE;
        }

        checkLog.setMsg("trade_status是无效值");
        log.info(checkLog.toString());
        return FAILURE;
    }

    private AlipayClient getAlipayClient() {
        if (alipayClient == null) {
            synchronized (AlipayClient.class) {
                if (alipayClient == null) {
                    alipayClient = new DefaultAlipayClient(alipayGateway, appId, appPrivateKey, format, charset, alipayPublicKey, signType);
                }
            }
        }
        return alipayClient;
    }
}
