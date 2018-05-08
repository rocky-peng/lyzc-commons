package com.leyongzuche.commons.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * @author pengqingsong
 * @desc json序列化和反序列化工具类
 */
public final class JsonUtils {

    private final static ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static <T> String serialize(T entity) {
        try {
            return OBJECT_MAPPER.writeValueAsString(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String value, Class<T> entityClass) {
        try {
            return OBJECT_MAPPER.readValue(value, entityClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> deserializeList(String value, Class<T> entityClass) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, entityClass);
            return OBJECT_MAPPER.readValue(value, javaType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}