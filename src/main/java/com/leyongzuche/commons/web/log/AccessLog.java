package com.leyongzuche.commons.web.log;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pengqingsong
 * 09/12/2017
 */
@Data
public class AccessLog {

    public static final Logger log = LoggerFactory.getLogger(AccessLog.class);

    private RequestLog request = new RequestLog();

    private Object response;

    private Long checkTokenTime;

    private Long controllerTime;
}

