package com.leyongzuche.commons.web;

import com.leyongzuche.commons.utils.HttpRequestUtils;
import com.leyongzuche.commons.utils.JsonUtils;
import com.leyongzuche.commons.web.log.AccessLog;
import com.leyongzuche.commons.web.log.RequestLog;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author pengqingsong
 * @date 08/05/2018
 * @desc
 */
@Slf4j
public class AccessLogUtils {

    private static void recordRequestLog(HttpServletRequest request) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        RequestLog requestLog = new RequestLog();
        requestLog.setTime(sdf.format(new Date()));
        requestLog.setUri(request.getRequestURI());
        requestLog.setParams(request.getParameterMap());
        requestLog.setMethod(request.getMethod());
        requestLog.setHost(HttpRequestUtils.getHost(request));
        requestLog.setClientIp(HttpRequestUtils.getRequestIp(request));
        requestLog.setHeaders(HttpRequestUtils.getHeaderMap(request));
        requestLog.setQueryString(request.getQueryString());

        AccessLog accessLog = new AccessLog();
        accessLog.setRequest(requestLog);

        request.setAttribute(AccessLog.class.getName(), accessLog);
    }

    public static void recordAccessLog(HttpServletRequest request, HttpServletResponse response, Object responseResult, Long checkTokenTime, Long controllerTime) {
        AccessLog accessLog = (AccessLog) request.getAttribute(AccessLog.class.getName());
        if (accessLog == null) {
            recordRequestLog(request);
            accessLog = (AccessLog) request.getAttribute(AccessLog.class.getName());
        }
        accessLog.setResponse(responseResult);
        accessLog.setCheckTokenTime(checkTokenTime);
        accessLog.setControllerTime(controllerTime);
        AccessLog.log.info(JsonUtils.serialize(accessLog));
    }
}
