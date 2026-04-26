package com.kuopan.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserInfoVO implements Serializable {
    private String userId;
    private String userName;
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regTime;
    private Boolean status;
    private Long occuSpace;
    private Long totalSpace;
    private Byte role;
}
