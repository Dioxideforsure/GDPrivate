package com.kuopan.Entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SysSettingsDto implements Serializable {
    private String registerEmailTitle = "邮箱验证码";

    private String registerEmailContent = "您好，您的邮箱验证码是：%s，8分钟有效";

    private Integer userInitialSpace = 5;
}
