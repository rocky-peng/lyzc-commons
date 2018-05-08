package com.leyongzuche.commons.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pengqingsong
 * @date 27/01/2018
 * @desc
 */
@Slf4j
public class BaiduMapUtils {

    private String ak;

    public BaiduMapUtils(String ak) {
        this.ak = ak;
    }

    public String getAddrByPoint(float latitude, float longitude) {
        if (!GpsUtils.isValidLatitude(latitude)) {
            throw new IllegalArgumentException("无效的纬度");
        }

        if (!GpsUtils.isValidLongitude(longitude)) {
            throw new IllegalArgumentException("无效的经度");
        }

        String location = latitude + "," + longitude;
        try {
            String url = "http://api.map.baidu.com/geocoder/v2/";

            Map<String, String> params = new HashMap<>();
            params.put("location", location);
            params.put("output", "json");
            params.put("pois", "0");
            params.put("ak", ak);
            String resultStr = HttpRequestUtils.post(url, params);
            Map map = JsonUtils.deserialize(resultStr, Map.class);
            if (CollectionUtils.isEmpty(map)) {
                return "";
            }

            Map resultMap = (Map) map.get("result");
            if (CollectionUtils.isEmpty(resultMap)) {
                return "";
            }

            return MapUtils.getString(resultMap, "formatted_address") + MapUtils.getString(resultMap, "sematic_description");
        } catch (Exception e) {
            log.error("根据经纬度获取地址信息失败[" + location + "]", e);
            return "";
        }
    }


}
