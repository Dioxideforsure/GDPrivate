package com.kuopan.Entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class SessionShareDto {
    private String shareId;
    private String shareUserId;
    private LocalDateTime expireTime;
    private String fileId;
}
