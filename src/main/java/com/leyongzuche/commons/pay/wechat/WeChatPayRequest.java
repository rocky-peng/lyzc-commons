package com.leyongzuche.commons.pay.wechat;

import com.leyongzuche.commons.utils.JsonUtils;
import lombok.Data;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WeChatPayRequest {

    private WeChatPayConfig config;
    private OkHttpClient certOkHttpClient;
    private OkHttpClient noCertOkHttpClient;


    public WeChatPayRequest(WeChatPayConfig config) {
        this.config = config;
        this.certOkHttpClient = buildCertOkHttpClient(config);
        this.noCertOkHttpClient = buildNoCertOkHttpClient(config);
    }


    private OkHttpClient buildNoCertOkHttpClient(WeChatPayConfig config) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(config.getHttpReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).build();
        return okHttpClient;
    }

    private OkHttpClient buildCertOkHttpClient(WeChatPayConfig config) {
        try {
            InputStream ins = WeChatPayRequest.class.getResourceAsStream(config.getPkcs12FilePath());
//            InputStream ins = new FileInputStream(config.getPkcs12FilePath());
            SSLSocketFactory sslSocketFactory = buildSSLSocketFactory(ins, config.getPkcs12FilePasswd());
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(config.getHttpReadTimeoutMs(), TimeUnit.MILLISECONDS)
                    .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS).sslSocketFactory(sslSocketFactory).build();
            return okHttpClient;
        } catch (Exception e) {
            return null;
        }
    }

    private SSLSocketFactory buildSSLSocketFactory(InputStream ins, String pkcs12FilePasswd) {
        try {
            char[] passwd = pkcs12FilePasswd.toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(ins, passwd);
            SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, passwd).build();
            SSLSocketFactory socketFactory = sslcontext.getSocketFactory();
            return socketFactory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 请求，只请求一次，不做重试
     *
     * @param checkReturnCode 是否检查return_code
     * @param checkRespSign   是否检查sign，只有checkReturnCode==true的情况下才生效
     * @param useCert         是否使用证书，针对退款、撤销等操作
     * @return null 表示请求异常
     */
    public Map<String, String> requestOnce(String urlSuffix, String data, boolean useCert, boolean checkReturnCode, boolean checkRespSign) {
        String url = "https://api.mch.weixin.qq.com" + urlSuffix;
        return requestOnceWithFullUrl(url, data, useCert, checkReturnCode, checkRespSign);
    }

    /**
     * 请求，只请求一次，不做重试
     *
     * @param checkReturnCode 是否检查return_code
     * @param checkRespSign   是否检查sign，只有checkReturnCode==true的情况下才生效
     * @param useCert         是否使用证书，针对退款、撤销等操作
     * @return null 表示请求异常
     */
    public Map<String, String> requestOnceWithFullUrl(String url, String data, boolean useCert, boolean checkReturnCode, boolean checkRespSign) {
        OkHttpClient okHttpClient;
        if (useCert) {
            okHttpClient = this.certOkHttpClient;
        } else {
            okHttpClient = this.noCertOkHttpClient;
        }

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .addHeader("Content-Type", "text/xml")
                .addHeader("User-Agent", "wxpay sdk java v1.0 " + config.getMchID())
                .post(body)
                .url(url)
                .build();

        Exception ex = null;
        String responseStr = null;
        try {
            Response response = okHttpClient.newCall(request).execute();
            responseStr = response.body().string();
        } catch (Exception e) {
            ex = e;
        }

        //记录日志
        WeChatPayLog payLog = new WeChatPayLog();
        payLog.setApi(url);
        payLog.setParams(data);
        payLog.setUseCert(useCert);
        payLog.setRespStr(responseStr);
        payLog.setEx(ex);
        payLog.setCheckRespSign(checkRespSign);
        payLog.setCheckReturnCode(checkReturnCode);

        if (ex != null) {
            failLogAndThrowException(payLog);
        }

        String returnCodeKey = "return_code";
        Map<String, String> respMap = WeChatPayUtils.xmlToMap(responseStr);
        payLog.setRespMap(respMap);

        if (!checkRespSign) {
            successLog(payLog);
            return respMap;
        }

        if (!respMap.containsKey(returnCodeKey)) {
            payLog.setErrMsg("请求微信支付相关接口返回结果中没有`return_code`");
            failLogAndThrowException(payLog);
        }

        String returnCode = respMap.get(returnCodeKey);
        if (returnCode.equals(WeChatPayAPI.FAIL)) {
            payLog.setErrMsg("请求微信支付相关接口返回结果中`return_code`为失败:" + returnCode);
            failLogAndThrowException(payLog);
        }

        if (!returnCode.equals(WeChatPayAPI.SUCCESS)) {
            payLog.setErrMsg("请求微信支付相关接口返回结果中`return_code`无效:" + returnCode);
            failLogAndThrowException(payLog);
        }

        if (checkRespSign) {
            String sign = respMap.get(WeChatPayAPI.FIELD_SIGN);
            if (StringUtils.isBlank(sign)) {
                payLog.setErrMsg("请求微信支付相关接口返回结果中`sign`值为空或空字符串:" + sign);
                failLogAndThrowException(payLog);
            }

            String expectSign = WeChatPayUtils.genSign(respMap, config.getKey(), config.getSignType());
            if (!sign.equals(expectSign)) {
                payLog.setErrMsg("请求微信支付相关接口返回结果中`sign`值无效,sign:" + sign + ",expectSign:" + expectSign);
                failLogAndThrowException(payLog);
            }
        }

        successLog(payLog);
        return respMap;
    }

    private void successLog(WeChatPayLog payLog) {
        WeChatPayAPI.log.info("请求微信支付相关接口成功[" + JsonUtils.serialize(payLog) + "]");
    }

    private void failLogAndThrowException(WeChatPayLog payLog) {
        if (payLog.getEx() != null) {
            WeChatPayAPI.log.error("请求微信支付相关接口失败[" + JsonUtils.serialize(payLog) + "]", payLog.getEx());
        } else {
            WeChatPayAPI.log.error("请求微信支付相关接口失败[" + JsonUtils.serialize(payLog) + "]");
        }
        throw new RuntimeException("请求微信支付相关接口失败");
    }

    /**
     * 企业账户转账到个人用户
     */
    public Map<String, String> fundTransfer(String data) {
        return requestOnce(WeChatPayAPI.FUND_TRANSFER_URL_SUFFIX, data, true, true, false);
    }

    @Data
    private class WeChatPayLog {

        private String api;

        private String params;

        private String respStr;

        private Exception ex;

        private boolean useCert;

        private String errMsg;

        private Map<String, String> respMap;

        private boolean checkReturnCode;

        private boolean checkRespSign;
    }
}
