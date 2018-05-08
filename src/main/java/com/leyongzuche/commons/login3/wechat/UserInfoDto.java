package com.leyongzuche.commons.login3.wechat;

import com.leyongzuche.commons.enums.GenderEnum;
import lombok.Data;

/**
 * @author pengqingsong
 * @date 26/09/2017
 * @desc
 */
@Data
public class UserInfoDto {

    private String openId;

    private String nickName;

    private GenderEnum gender;

    private String language;

    private String city;

    private String province;

    private String country;

    private String headImgUrl;

    private Object privilege;

    private String unionId;
}
