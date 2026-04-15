package com.kuopan.Entity.enums;

import lombok.Getter;

@Getter
public enum ResponseCodeEnum {
    CODE_200(200, "请求成功"),
    CODE_403(403, "拒绝访问"),
    CODE_404(404, "请求地址不存在"),
    CODE_600(600, "请求参数错误"),
    CODE_601(601, "信息已经存在"),
    CODE_500(500, "服务器返回错误，请联系管理员"),
    CODE_901(901, "登录超时，请重新登录"),
    CODE_904(904, "网盘空间不足");

    private Integer code;
    private String message;

    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.message = msg;
    }

}

