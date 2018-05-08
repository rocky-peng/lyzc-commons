package com.leyongzuche.commons.web;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author :  pengqingsong
 * Date : 02/03/2017 11:48
 * Description : 通用的响应封装类
 * Test :
 */
@Data
public class GeneralResponse<T> implements Serializable {

    private static final String SUCCESS_MSG = "success";
    private static final String UNKNOWN_EXCEPTION = "unknown exception";
    public static final String NO_PRIVILEGE_MSG = "您无权进行此操作";
    private static final String SERVER_ERROR = "服务器异常,请稍后再试";
    public static final String LOGIN_REQUIRED = "请先登录";

    public static final GeneralResponse EMPTY_SUCCESS_RESPONSE = new GeneralResponse(SUCCESS_MSG);
    public static final GeneralResponse NO_PRIVILEGE_RESPONSE = new GeneralResponse(NO_PRIVILEGE_MSG);
    public static final GeneralResponse SERVER_ERROR_RESPONSE = new GeneralResponse(SERVER_ERROR);
    /**
     * 辅助消息，如果为success表示请求处理成功，否则说明请求处理不成功
     */
    private String msg;
    /**
     * 返回的数据
     */
    private T data;

    private GeneralResponse() {
    }

    private GeneralResponse(String msg) {
        this.msg = msg;
    }


    public static <T> GeneralResponse<T> successResponse(T data) {
        GeneralResponse response = new GeneralResponse();
        response.setMsg(SUCCESS_MSG);
        response.setData(data);
        return response;
    }

    public static <T> GeneralResponse<T> failedResponse(String msg) {
        GeneralResponse response = new GeneralResponse();
        response.setMsg(msg);
        return response;
    }

    public static <T> GeneralResponse<T> unknownExceptionResponse(Throwable e) {
        GeneralResponse response = new GeneralResponse();
        response.setMsg(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
        return response;
    }

    public static boolean isSuccess(GeneralResponse response) {
        return response != null && Objects.equals(SUCCESS_MSG, response.getMsg());
    }
}
