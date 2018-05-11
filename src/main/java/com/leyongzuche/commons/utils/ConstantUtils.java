package com.leyongzuche.commons.utils;

import okhttp3.OkHttpClient;
import sun.misc.BASE64Encoder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 常量工具类
 *
 * @author pengqingsong
 * 11/09/2017
 */
public class ConstantUtils {

    public static final long SECOND_IN_ONE_MINUTE = 60;

    public static final long SECOND_IN_ONE_HOUR = 3600;

    public static final long SECOND_IN_6HOURS = 6 * 3600;

    public static final long SECOND_IN_8HOURS = 8 * 3600;

    public static final long SECOND_IN_ONE_DAY = 24 * 3600;

    public static final long MILLS_IN_ONE_SECOND = 1000;

    public static final String RECHARGE_ORDER_PREFIX = "ly-recharge-";

    public static final ThreadPoolExecutor EXECUTORS;
    public static final ThreadLocal<BASE64Encoder> BASE_64_ENCODER_THREAD_LOCAL = ThreadLocal.withInitial(() -> new BASE64Encoder());
    public static final ThreadLocal<OkHttpClient> OK_HTTP_CLIENT_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).build();
        return okHttpClient;
    });
    public static final ThreadLocal<OkHttpClient> OK_HTTP_CLIENT_WITH_SSL_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(getSSLSocketFactory())
                .writeTimeout(10, TimeUnit.SECONDS).build();
        return okHttpClient;
    });

    static {
        EXECUTORS = new ThreadPoolExecutor(100, 100,
                1L, TimeUnit.HOURS,
                new LinkedBlockingQueue<Runnable>());
        EXECUTORS.allowCoreThreadTimeOut(true);
    }

    private static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
