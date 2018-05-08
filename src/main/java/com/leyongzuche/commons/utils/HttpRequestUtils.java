package com.leyongzuche.commons.utils;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pengqingsong
 * @date 07/09/2017
 * @desc http请求工具类
 */
public class HttpRequestUtils {

    public static String post(String url, Map<String, String> queryParams) {
//        Request post = Request.Post(url);
//
//        if (queryParams != null && queryParams.size() > 0) {
//            Form form = Form.form();
//            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
//                form.add(entry.getKey(), entry.getValue());
//            }
//            List<NameValuePair> nameValuePairs = form.build();
//            post.bodyForm(nameValuePairs, Charset.forName("utf8"));
//        }
//        try {
//            Response response = post.execute();
//            Content content = response.returnContent();
//            return content.asString();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        String str = "";
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            str += (entry.getKey() + "=" + entry.getValue() + "&");
        }
        str = str.substring(0, str.length() - 1);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), str))
                .build();
        return executeRequest(request, false);
    }


    public static String post(String url, String content, String mediaType) {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse(mediaType), content))
                .build();
        return executeRequest(request, false);
    }

    public static String postHttps(String url, Map<String, String> params) {
        String str = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            str += (entry.getKey() + "=" + entry.getValue() + "&");
        }
        str = str.substring(0, str.length() - 1);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), str))
                .build();
        return executeRequest(request, true);
    }


    private static String executeRequest(okhttp3.Request request, boolean ssl) {
        try {
            OkHttpClient okHttpClient = null;
            if (ssl) {
                okHttpClient = ConstantUtils.OK_HTTP_CLIENT_WITH_SSL_THREAD_LOCAL.get();
            } else {
                okHttpClient = ConstantUtils.OK_HTTP_CLIENT_THREAD_LOCAL.get();
            }

            okhttp3.Response response = okHttpClient.newCall(request).execute();
            int code = response.code();
            String bodyStr = response.body().string();
            if (code != 200) {
                throw new RuntimeException(request.method() + "请求失败,url[" + request.url() + "],response[" + bodyStr + "]");
            }
            return bodyStr;
        } catch (Exception e) {
            throw new RuntimeException(request.method() + "请求失败,url[" + request.url() + "]", e);
        }
    }

    public static String get(String url) {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
        return executeRequest(request, false);
    }

    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "utf8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHost(HttpServletRequest request) {
        String host = request.getHeader("host");
        if (StringUtils.isBlank(host)) {
            host = request.getHeader("HOST");
        }
        return host;
    }

    public static String getRequestIp(HttpServletRequest request) {
        String unknow = "unknown";

        String ipAddress = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ipAddress) || unknow.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ipAddress) || unknow.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ipAddress) || unknow.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                ipAddress = inet.getHostAddress();
            }
        }
        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        //"***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > 15) {
            int index = ipAddress.indexOf(",");
            if (index > 0) {
                ipAddress = ipAddress.substring(0, index);
            }
        }
        return ipAddress;
    }

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerKey = headerNames.nextElement();
            String headerValue = request.getHeader(headerKey);
            result.put(headerKey, headerValue);
        }

        return result;
    }

    public static void sendFileToClient(String path, HttpServletResponse response) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }

        try {
            String filename = file.getName();
            filename = URLEncoder.encode(filename, "utf8");
            InputStream fis = new BufferedInputStream(new FileInputStream(path));
            response.reset();
            response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
            response.addHeader("Content-Length", "" + file.length());
            response.setContentType("application/octet-stream");
            IOUtils.copy(fis, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendFileToClient(InputStream is, HttpServletResponse response, String fileName) {
        try {
            String filename = fileName;
            filename = URLEncoder.encode(filename, "utf8");
            response.reset();
            response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
            response.addHeader("Content-Length", "" + is.available());
            response.setContentType("application/octet-stream");
            IOUtils.copy(is, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
