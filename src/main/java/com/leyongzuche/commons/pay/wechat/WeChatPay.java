package com.leyongzuche.commons.pay.wechat;

import com.leyongzuche.commons.encrpt.RSAEncrypt;
import com.leyongzuche.commons.pay.PayBusinessCallback;
import com.leyongzuche.commons.pay.PayTypeEnum;
import com.leyongzuche.commons.utils.HttpRequestUtils;
import com.leyongzuche.commons.utils.JsonUtils;
import com.leyongzuche.commons.utils.MiscUtils;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pengqingsong
 * 18/10/2017
 */
@Slf4j
public class WeChatPay implements InitializingBean {

    @Setter
    private WeChatPayConfig config;

    @Setter
    private PayBusinessCallback payBusinessCallback;

    private WeChatPayRequest weChatPayRequest;

    private RSAEncrypt rsaEncrypt;

    public WeChatPay(WeChatPayConfig config, PayBusinessCallback payBusinessCallback) {
        this.config = config;
        this.payBusinessCallback = payBusinessCallback;
        doInit();
    }

    public WeChatPay() {

    }

    private void doInit() {
        this.weChatPayRequest = new WeChatPayRequest(config);
        rsaEncrypt = buildRsaEncrypt(config);
    }

    public void setConfig(WeChatPayConfig config) {
        this.config = config;
    }

    public void setPayBusinessCallback(PayBusinessCallback payBusinessCallback) {
        this.payBusinessCallback = payBusinessCallback;
    }

    private RSAEncrypt buildRsaEncrypt(WeChatPayConfig config) {
        if (StringUtils.isBlank(config.getPkcs8FilePath())) {
            return null;
        }
        try {
            InputStream ins = WeChatPayRequest.class.getResourceAsStream(config.getPkcs8FilePath());
//            InputStream ins = new FileInputStream(config.getPkcs8FilePath());
            return new RSAEncrypt(ins);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 微信支付统一下单接口
     *
     * @param totalMoney     订单总金额，单位元，精确到分
     * @param clientIp       客户端的IP
     * @param orderTimeMs    下单时间：业务系统的下单时间
     * @param payValidTimeMs 支付有效时间
     * @param userOpenId     用户的openId
     */
    public Map<String, String> makeOrder(String orderName, String orderCode,
                                         BigDecimal totalMoney, String clientIp,
                                         long orderTimeMs, long payValidTimeMs,
                                         String userOpenId) {
        Map<String, String> resqMap = new HashMap<>();
        //商品描述
        resqMap.put("body", orderName);

        //商户订单号(系统内部订单唯一编码)
        resqMap.put("out_trade_no", orderCode);

        totalMoney = totalMoney.multiply(new BigDecimal(100));
        totalMoney = totalMoney.setScale(0, BigDecimal.ROUND_HALF_UP);
        //订单总金额，单位为分
        resqMap.put("total_fee", String.valueOf(totalMoney.intValue()));
        //终端IP
        resqMap.put("spbill_create_ip", clientIp);
        //订单生成时间（本系统订单生成时间）
        resqMap.put("time_start", WeChatPayUtils.dateFormat(orderTimeMs));
        //交易结束时间
        resqMap.put("time_expire", WeChatPayUtils.dateFormat(payValidTimeMs));
        //交易类型
        resqMap.put("trade_type", "JSAPI");
        resqMap.put("openid", userOpenId);

        if (config.getNotifyUrl() != null) {
            resqMap.put("notify_url", config.getNotifyUrl());
        }

        addGeneralRequestParams(resqMap);

        String reqBody = WeChatPayUtils.mapToXml(resqMap);

        Map<String, String> respMap = weChatPayRequest.requestOnce(WeChatPayAPI.UNIFIEDORDER_URL_SUFFIX, reqBody, false, true, true);

        String resultCode = respMap.get("result_code");
        if (!"SUCCESS".equals(resultCode)) {
            throw new RuntimeException("微信统一下单失败，result_code=" + resultCode);
        }

        String prepayId = respMap.get("prepay_id");

        Map<String, String> data = new HashMap<>(10);
        data.put("appId", config.getAppId());
        data.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        data.put("nonceStr", MiscUtils.randomUUID());
        data.put("package", "prepay_id=" + prepayId);
        data.put("signType", "MD5");
        data.put("paySign", WeChatPayUtils.genSign(data, config.getKey(), WeChatPaySignTypeEnum.MD5));
        return data;
    }

    private String encrypt(String str) {
        try {
            byte[] bytes = rsaEncrypt.encrypt(str.getBytes("utf8"));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 企业向个人转账的接口
     *
     * @param wxOpenId     目标用户的openId
     * @param realUserName 目标用户的身份证姓名
     * @param totalMoney   转账总金额，单位元，精确到分
     */
    public boolean fundTransferToWx(String wxOpenId, String realUserName, BigDecimal totalMoney, String fundDesc) {
        totalMoney = totalMoney.multiply(new BigDecimal(100));
        totalMoney = totalMoney.setScale(0, BigDecimal.ROUND_HALF_UP);
        String orderCode = MiscUtils.randomUUID();

        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("mch_appid", config.getAppId());
        reqMap.put("mchid", config.getMchID());
        reqMap.put("nonce_str", MiscUtils.randomUUID());
        reqMap.put("partner_trade_no", orderCode);
        reqMap.put("openid", wxOpenId);
        reqMap.put("check_name", "FORCE_CHECK");
        reqMap.put("re_user_name", realUserName);
        reqMap.put("amount", String.valueOf(totalMoney.intValue()));
        reqMap.put("desc", fundDesc);
        String localIp = HttpRequestUtils.getLocalIp();
        WeChatPayAPI.log.info(localIp);
        reqMap.put("spbill_create_ip", localIp);
        String sign = WeChatPayUtils.genSign(reqMap, config.getKey(), WeChatPaySignTypeEnum.MD5);
        reqMap.put("sign", sign);

        String reqBody = WeChatPayUtils.mapToXml(reqMap);
        return retryFundTransfer(reqBody, 0);
    }

    /**
     * 企业向个人转账的接口
     *
     * @param wxOpenId   目标用户的openId
     * @param totalMoney 转账总金额，单位元，精确到分
     */
    public boolean fundTransferToWx(String wxOpenId, BigDecimal totalMoney, String fundDesc) {
        totalMoney = totalMoney.multiply(new BigDecimal(100));
        totalMoney = totalMoney.setScale(0, BigDecimal.ROUND_HALF_UP);
        String orderCode = MiscUtils.randomUUID();

        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("mch_appid", config.getAppId());
        reqMap.put("mchid", config.getMchID());
        reqMap.put("nonce_str", MiscUtils.randomUUID());
        reqMap.put("partner_trade_no", orderCode);
        reqMap.put("openid", wxOpenId);
        reqMap.put("check_name", "NO_CHECK");
        reqMap.put("amount", String.valueOf(totalMoney.intValue()));
        reqMap.put("desc", fundDesc);
        String localIp = HttpRequestUtils.getLocalIp();
        WeChatPayAPI.log.info(localIp);
        reqMap.put("spbill_create_ip", localIp);
        String sign = WeChatPayUtils.genSign(reqMap, config.getKey(), WeChatPaySignTypeEnum.MD5);
        reqMap.put("sign", sign);

        String reqBody = WeChatPayUtils.mapToXml(reqMap);
        return retryFundTransfer(reqBody, 0);
    }

    /**
     * 企业转账到银行
     *
     * @param transferBankOrderCode 订单编号
     * @param bankNo                银行卡号
     * @param trueName              真实姓名
     * @param bankCode              银行编号
     * @param transferMoney         转账金额（单位元）
     * @param transferDesc          转账描述
     */
    public void fundTransferToBank(String transferBankOrderCode, String bankNo, String trueName, String bankCode, BigDecimal transferMoney, String transferDesc) {
        long s = System.currentTimeMillis();
        try {
            transferMoney = transferMoney.multiply(new BigDecimal(100));
            transferMoney = transferMoney.setScale(0, BigDecimal.ROUND_HALF_UP);
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("mch_id", config.getMchID());
            reqMap.put("partner_trade_no", transferBankOrderCode);
            reqMap.put("nonce_str", MiscUtils.randomUUID());
            reqMap.put("enc_bank_no", encrypt(bankNo));
            reqMap.put("enc_true_name", encrypt(trueName));
            reqMap.put("bank_code", bankCode);
            reqMap.put("amount", String.valueOf(transferMoney.intValue()));
            reqMap.put("desc", transferDesc);
            String sign = WeChatPayUtils.genSign(reqMap, config.getKey(), WeChatPaySignTypeEnum.MD5);
            reqMap.put("sign", sign);

            String reqBody = WeChatPayUtils.mapToXml(reqMap);
            String url = "https://api.mch.weixin.qq.com/mmpaysptrans/pay_bank";
            Map<String, String> map = weChatPayRequest.requestOnceWithFullUrl(url, reqBody, true, true, false);
            if (CollectionUtils.isEmpty(map)) {
                throw new RuntimeException("微信转账到银行卡失败");
            }
            String resultCode = map.get("result_code");
            if (!"SUCCESS".equals(resultCode)) {
                String msg = map.get("err_code_des");
                if (StringUtils.isBlank(msg)) {
                    msg = "微信转账到银行卡失败";
                }
                log.error(msg + "[" + JsonUtils.serialize(map) + "]");
                throw new RuntimeException(msg);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            log.info("请求微信转账到银行卡的接口耗时：" + (System.currentTimeMillis() - s) + "s");
        }
    }

    /**
     * 企业转账到银行卡结果查询
     */
    public TransferToBankResult queryFundTransferToBankResult(String transferBankOrderCode) {
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("mch_id", config.getMchID());
        reqMap.put("partner_trade_no", transferBankOrderCode);
        reqMap.put("nonce_str", MiscUtils.randomUUID());
        String sign = WeChatPayUtils.genSign(reqMap, config.getKey(), WeChatPaySignTypeEnum.MD5);
        reqMap.put("sign", sign);

        String reqBody = WeChatPayUtils.mapToXml(reqMap);
        String url = "https://api.mch.weixin.qq.com/mmpaysptrans/query_bank";
        Map<String, String> map = weChatPayRequest.requestOnceWithFullUrl(url, reqBody, true, true, false);
        if (CollectionUtils.isEmpty(map)) {
            throw new RuntimeException("企业转账到银行卡结果查询失败");
        }
        String resultCode = map.get("result_code");
        if (!"SUCCESS".equals(resultCode)) {
            String errCodeDes = map.get("err_code_des");
            String returnMsg = map.get("return_msg");
            String reason = StringUtils.isBlank(errCodeDes) ? returnMsg : errCodeDes;
            return new TransferToBankResult(TransferToBankResultEnum.FAILED, reason, "null");
        } else {
            String status = map.get("status");
            TransferToBankResultEnum resultEnum = TransferToBankResultEnum.valueOf(status);
            String paySuccTime = map.get("pay_succ_time");
            String reason = map.get("reason");
            return new TransferToBankResult(resultEnum, reason, paySuccTime);
        }
    }

    public String getPublicKey() {
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("mch_id", config.getMchID());
        reqMap.put("nonce_str", MiscUtils.randomUUID());
        reqMap.put("sign_type", "MD5");
        String sign = WeChatPayUtils.genSign(reqMap, config.getKey(), WeChatPaySignTypeEnum.MD5);
        reqMap.put("sign", sign);
        String reqBody = WeChatPayUtils.mapToXml(reqMap);
        Map<String, String> map = weChatPayRequest.requestOnceWithFullUrl("https://fraud.mch.weixin.qq.com/risk/getpublickey", reqBody, true, true, false);
        if (CollectionUtils.isEmpty(map)) {
            throw new RuntimeException("请求微信支付公钥失败");
        }

        String resultCode = map.get("result_code");
        if (!"SUCCESS".equals(resultCode)) {
            throw new RuntimeException("请求微信支付公钥失败");
        }

        String pubKey = map.get("pub_key");
        return pubKey;
    }


    /**
     * 申请退款
     *
     * @param orderCode       对哪个订单申请退款（自己系统的订单）
     * @param orderTotalMoney 订单的总金额，单位元
     * @param refundMoney     退款总金额，单位元
     * @param refundDesc      退款的描述信息
     * @return 是否退款成功
     */
    public boolean refund(String orderCode, BigDecimal orderTotalMoney, BigDecimal refundMoney, String refundDesc) {
        orderTotalMoney = orderTotalMoney.multiply(new BigDecimal(100));
        orderTotalMoney = orderTotalMoney.setScale(0, BigDecimal.ROUND_HALF_UP);

        refundMoney = refundMoney.multiply(new BigDecimal(100));
        refundMoney = refundMoney.setScale(0, BigDecimal.ROUND_HALF_DOWN);

        Map<String, String> resqMap = new HashMap<>();
        resqMap.put("out_trade_no", orderCode);
        resqMap.put("out_refund_no", MiscUtils.randomUUID());
        resqMap.put("total_fee", String.valueOf(orderTotalMoney.intValue()));
        resqMap.put("refund_fee", String.valueOf(refundMoney.intValue()));

        if (!StringUtils.isBlank(refundDesc)) {
            resqMap.put("refund_desc", refundDesc);
        }

        addGeneralRequestParams(resqMap);

        String reqBody = WeChatPayUtils.mapToXml(resqMap);
        return retryRefund(reqBody, 0);
    }

    /**
     * 重复尝试3次退款申请
     */
    private boolean retryRefund(String reqBody, int tryCount) {
        Map<String, String> respMap = weChatPayRequest.requestOnce(WeChatPayAPI.REFUND_URL_SUFFIX, reqBody, true, true, true);

        String resultCode = respMap.get("result_code");
        if (!"SUCCESS".equals(resultCode)) {
            tryCount++;
            if (tryCount >= 3) {
                String errMsg = "微信申请退款重试" + tryCount + "次都失败，返回数据：" + JsonUtils.serialize(respMap);
                WeChatPayAPI.log.error(errMsg);
                return false;
            }
            return retryRefund(reqBody, tryCount);
        }

        WeChatPayAPI.log.info("微信申请退款成功");
        return true;
    }

    /**
     * 重复尝试3次企业向个人转账
     */
    private boolean retryFundTransfer(String reqBody, int tryCount) {
        Map<String, String> respMap = weChatPayRequest.fundTransfer(reqBody);

        String errCode = respMap.get("err_code");
        String resultCode = respMap.get("result_code");
        if ("SYSTEMERROR".equals(errCode) || !"SUCCESS".equals(resultCode)) {
            tryCount++;
            if (tryCount >= 3) {
                WeChatPayAPI.log.error("微信企业转账到个人重试" + tryCount + "次都失败[err_code:" + errCode + ",result_code:" + resultCode + "]");

                //todo 给管理员发送短信
                return false;
            }
            return retryFundTransfer(reqBody, tryCount);
        }

        WeChatPayAPI.log.info("微信企业转账到个人成功");
        return true;
    }

    /**
     * 微信支付回调接口
     *
     * @return 返回的字符串直接返回给微信服务器
     */
    public String callback(String xmlParms) {

        Map<String, String> mapParams = WeChatPayUtils.xmlToMap(xmlParms);

        CallBackLog callBackLog = new CallBackLog();
        callBackLog.setXmlParams(xmlParms);

        String sign = mapParams.remove("sign");
        if (StringUtils.isBlank(sign)) {
            callBackLog.setMsg("回调参数中sign无效");
            failLogAndThrowException(callBackLog);
        }

        String expectSign = WeChatPayUtils.genSign(mapParams, config.getKey(), config.getSignType());
        if (!sign.equals(expectSign)) {
            callBackLog.setMsg("回调参数中sign无效,expectSign:" + expectSign);
            failLogAndThrowException(callBackLog);
        }

        mapParams.put("sign", sign);
        String returnCode = mapParams.get("return_code");
        if (!"SUCCESS".equals(returnCode)) {
            callBackLog.setMsg("回调参数中return_code无效");
            failLogAndThrowException(callBackLog);
        }

        String appid = mapParams.get("appid");
        if (!config.getAppId().equals(appid)) {
            callBackLog.setMsg("回调参数中appid非法，expectAppId:" + config.getAppId());
            failLogAndThrowException(callBackLog);
        }

        String orderCode = mapParams.get("out_trade_no");
        if (StringUtils.isBlank(orderCode)) {
            callBackLog.setMsg("回调参数中out_trade_no无效");
            failLogAndThrowException(callBackLog);
        }

        String transactionId = mapParams.get("transaction_id");
        if (StringUtils.isBlank(transactionId)) {
            callBackLog.setMsg("回调参数中transaction_id无效");
            failLogAndThrowException(callBackLog);
        }

        long orderId = payBusinessCallback.getOrderIdByOrderCode(orderCode);
        if (orderId <= 0) {
            callBackLog.setMsg("回调参数中out_trade_no在数据库中不存在");
            failLogAndThrowException(callBackLog);
        }

        if (!payBusinessCallback.doWhenTradeSuccess(orderCode, PayTypeEnum.WEIXIN_ONLINE, transactionId)) {
            callBackLog.setMsg("更改数据库中订单状态，失败");
            failLogAndThrowException(callBackLog);
        }

        callBackLog.setMsg("更改数据库中订单状态，成功");
        WeChatPayAPI.log.info("处理微信回调参数，成功,[" + JsonUtils.serialize(callBackLog) + "]");
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    private void failLogAndThrowException(CallBackLog callBackLog) {
        if (callBackLog.getEx() == null) {
            WeChatPayAPI.log.error("处理微信回调参数，异常,[" + JsonUtils.serialize(callBackLog) + "]");
        } else {
            WeChatPayAPI.log.error("处理微信回调参数异常,[" + JsonUtils.serialize(callBackLog) + "]", callBackLog.getEx());
        }
        throw new RuntimeException("处理微信回调参数异常");
    }

    /**
     * 向 Map 中添加 appid、mch_id、nonce_str、sign_type、sign <br>
     * 该函数适用于商户适用于统一下单等接口，不适用于红包、代金券接口
     *
     * @param reqData
     * @return
     * @throws Exception
     */
    private void addGeneralRequestParams(Map<String, String> reqData) {
        reqData.put("appid", config.getAppId());
        reqData.put("mch_id", config.getMchID());
        reqData.put("nonce_str", MiscUtils.randomUUID());
        if (WeChatPaySignTypeEnum.MD5.equals(config.getSignType())) {
            reqData.put("sign_type", WeChatPayAPI.MD5);
        } else if (WeChatPaySignTypeEnum.HMACSHA256.equals(config.getSignType())) {
            reqData.put("sign_type", WeChatPayAPI.HMACSHA256);
        }
        reqData.put("sign", WeChatPayUtils.genSign(reqData, config.getKey(), config.getSignType()));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        doInit();
    }

    @Data
    private class CallBackLog {
        private String xmlParams;

        private Exception ex;

        private String msg;

        private Map<String, String> mapParams;
    }
}
