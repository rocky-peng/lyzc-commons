package com.leyongzuche.commons.login3.wechat;

import com.leyongzuche.commons.enums.GenderEnum;
import com.leyongzuche.commons.utils.HttpRequestUtils;
import com.leyongzuche.commons.utils.JsonUtils;
import com.leyongzuche.commons.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author pengqingsong
 * @date 26/09/2017
 * @desc
 */
@Slf4j
public class WeChatWebLoginUtils {

    private static final String ACCESS_TOKEN_URL_PATTEN = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    private static final String REFRESH_TOKEN_URL_PATTEN = "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=%s&grant_type=refresh_token&refresh_token=%s";
    private static final String USER_INFO_URL_PATTEN = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN";
    private String appId;
    private String appSecret;

    public WeChatWebLoginUtils(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public AccessTokenDto getAccessTokenByCode(String code) {
        String accessTokenUrl = String.format(ACCESS_TOKEN_URL_PATTEN, appId, appSecret, code);
        return getAccessTokenDto(accessTokenUrl);
    }

    public AccessTokenDto refreshAccessToken(String oldRefreshToken) {
        String refreshAccessTokenUrl = String.format(REFRESH_TOKEN_URL_PATTEN, appId, oldRefreshToken);
        return getAccessTokenDto(refreshAccessTokenUrl);
    }

    public UserInfoDto getUserInfo(String accessToken, String openId) {
        String userInfoUrl = String.format(USER_INFO_URL_PATTEN, accessToken, openId);
        String post = HttpRequestUtils.post(userInfoUrl, null);
        if (StringUtils.isBlank(post)) {
            log.error("请求url[" + userInfoUrl + "]失败");
            return null;
        }

        try {
            post = new String(post.getBytes("ISO-8859-1"), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map userInfoMap = JsonUtils.deserialize(post, Map.class);
        if (userInfoMap.containsKey("errmsg")) {
            log.error("请求url[" + userInfoUrl + "]失败,resp[" + post + "]");
            return null;
        }

        UserInfoDto result = convertMapToUserInfoDto(userInfoMap);
        return result;
    }

    private AccessTokenDto getAccessTokenDto(String accessTokenUrl) {
        String accessTokenResp = HttpRequestUtils.post(accessTokenUrl, null);
        if (StringUtils.isBlank(accessTokenResp)) {
            log.error("请求url[" + accessTokenUrl + "]失败");
            return null;
        }
        Map<String, Object> respMap = JsonUtils.deserialize(accessTokenResp, Map.class);
        if (!respMap.containsKey("access_token")) {
            log.error("请求url[" + accessTokenUrl + "]失败,resp[" + accessTokenResp + "]");
            return null;
        }
        AccessTokenDto dto = convertMapToAccessTokenDto(respMap);
        return dto;
    }

    private AccessTokenDto convertMapToAccessTokenDto(Map<String, Object> respMap) {
        String accessToken = MapUtils.getString(respMap, "access_token");
        int expiresIn = MapUtils.getInteger(respMap, "expires_in");
        String refreshToken = MapUtils.getString(respMap, "refresh_token");
        String openId = MapUtils.getString(respMap, "openid");
        String scope = MapUtils.getString(respMap, "scope");
        AccessTokenDto dto = new AccessTokenDto();
        dto.setAccessToken(accessToken);
        dto.setExpiresIn(expiresIn);
        dto.setRefreshToken(refreshToken);
        dto.setOpenId(openId);
        dto.setScope(scope);
        return dto;
    }

    private UserInfoDto convertMapToUserInfoDto(Map userInfoMap) {
        String openid = MapUtils.getString(userInfoMap, "openid");
        String nickname = MapUtils.getString(userInfoMap, "nickname");
        String sex = MapUtils.getString(userInfoMap, "sex");
        String language = MapUtils.getString(userInfoMap, "language");
        String city = MapUtils.getString(userInfoMap, "city");
        String province = MapUtils.getString(userInfoMap, "province");
        String country = MapUtils.getString(userInfoMap, "country");
        String headimgurl = MapUtils.getString(userInfoMap, "headimgurl");
        String unionid = MapUtils.getString(userInfoMap, "unionid");
        Object privilege = MapUtils.getObject(userInfoMap, "privilege");

        UserInfoDto result = new UserInfoDto();
        result.setOpenId(openid);
        result.setNickName(nickname);
        result.setGender(StringUtils.isBlank(sex) ? GenderEnum.UNKNOW : ("1".equals(sex) ? GenderEnum.MALE : GenderEnum.FEMALE));
        result.setLanguage(language);
        result.setCity(city);
        result.setProvince(province);
        result.setCountry(country);
        result.setHeadImgUrl(headimgurl);
        result.setPrivilege(privilege);
        result.setUnionId(unionid);
        return result;
    }
}
