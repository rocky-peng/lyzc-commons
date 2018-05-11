package com.leyongzuche.commons.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将map转换为对象的工具类【并不适合所有场景】
 *
 * @author pengqingsong
 * 16/10/2017
 */
@Slf4j
public class MapToBeanUtils {

    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]+");
    private static Map<Class, Map<String, Field>> fieldMapCache = new HashMap<>();

    public static <T> T convertMapToBean(Map<String, Object> map, Class<T> clazz) {
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }

        try {
            Map<String, Field> fieldMap = getFieldMap(clazz);
            T t = clazz.newInstance();
            fillData(t, map, fieldMap);
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> void fillData(T t, Map<String, Object> map, Map<String, Field> fieldMap) throws IllegalAccessException {
        if (fieldMap.size() <= map.size()) {
            for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
                Field field = entry.getValue();
                String dbColumnName = entry.getKey();

                Object value = MapUtils.getValue(map, dbColumnName, field.getType());
                if (value != null) {
                    field.set(t, value);
                }
            }
        } else {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object objectValue = entry.getValue();
                String dbColumnName = entry.getKey();

                Field field = fieldMap.get(dbColumnName);
                if (field == null) {
                    continue;
                }

                if (objectValue == null) {
                    field.set(t, objectValue);
                    continue;
                }

                if (objectValue.getClass() == field.getType()) {
                    field.set(t, objectValue);
                } else {
                    field.set(t, MapUtils.convert(objectValue, field.getType()));
                }
            }
        }
    }

    public static <T> List<T> convertMapToBean(List<Map<String, Object>> maps, Class<T> clazz) {
        if (CollectionUtils.isEmpty(maps)) {
            return Collections.EMPTY_LIST;
        }

        try {
            Map<String, Field> fieldMap = getFieldMap(clazz);
            List<T> result = new ArrayList<>(maps.size());
            for (Map<String, Object> map : maps) {
                T t = clazz.newInstance();
                fillData(t, map, fieldMap);
                result.add(t);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Map<String, Field> getFieldMap(Class<T> clazz) {
        Map<String, Field> fieldMap = fieldMapCache.get(clazz);
        if (fieldMap == null) {
            synchronized (fieldMapCache) {
                fieldMap = fieldMapCache.get(clazz);
                if (fieldMap == null) {
                    fieldMap = analyzeFieldMap(clazz);
                }
            }
        }
        return fieldMap;
    }

    private static Map<String, Field> analyzeFieldMap(Class clazz) {

        Field[] declaredFields = clazz.getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>(declaredFields.length);

        for (Field field : declaredFields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            String dbColumnName = getDbColumnName(fieldName);
            fieldMap.put(dbColumnName, field);
        }

        Class superClass = clazz.getSuperclass();

        if (superClass != Object.class) {
            Map<String, Field> superClassFieldMap = fieldMapCache.get(superClass);
            if (superClassFieldMap == null) {
                superClassFieldMap = analyzeFieldMap(superClass);
            }
            fieldMap.putAll(superClassFieldMap);
        }

        fieldMapCache.put(clazz, fieldMap);
        return fieldMap;
    }


    private static String getDbColumnName(String fieldName) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = UPPER_CASE_PATTERN.matcher(fieldName);
        int i = 1;
        while (matcher.find()) {
            String group = matcher.group();

            int index = fieldName.indexOf(group, i);
            sb.append(fieldName.substring(i - 1, index).toLowerCase()).append("_");

            i = index + 1;
        }
        sb.append(fieldName.substring(i - 1).toLowerCase());
        return sb.toString();
    }
}
