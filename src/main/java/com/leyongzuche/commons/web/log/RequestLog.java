package com.leyongzuche.commons.web.log;

import lombok.Data;

import java.util.Map;

/**
 * @author pengqingsong
 * @date 09/12/2017
 * @desc
 */
@Data
public class RequestLog {

    private Map<String, String> headers;

    private Map<String, String[]> params;

    private String queryString;

    private String clientIp;

    private String uri;

    private String method;

    private String time;

    private String host;
}
