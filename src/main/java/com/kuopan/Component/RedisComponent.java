package com.kuopan.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuopan.DAO.FileInfoMapper;
import com.kuopan.DAO.UserInfoMapper;
import com.kuopan.Entity.UserInfo;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.DownloadFileDto;
import com.kuopan.Entity.dto.SysSettingsDto;
import com.kuopan.Entity.dto.UserSpaceDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    private RedisTemplate<String, Object> searchAndUseInRedis;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    public SysSettingsDto getSysSettingDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) searchAndUseInRedis.opsForValue().get(Constants.REDIS_KEY_SYS_SETTINGS);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_SYS_SETTINGS, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    public void saveSysSettingsDto(SysSettingsDto dto) {
        searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_SYS_SETTINGS, dto);
    }

    public void saveUserUsedSpace(String userId, UserSpaceDto userSpaceDto) {
        searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.SEVEN, TimeUnit.DAYS);
    }

    public UserSpaceDto resetUserUsedSpace(String userId) {
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        Long usedSpace = fileInfoMapper.selectUsedSpace(userId);
        if (usedSpace == null) {
            usedSpace = 0L;
        }
        userSpaceDto.setUseSpace(usedSpace);
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId));

        if (userInfo != null) {
            userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        } else {
            userSpaceDto.setTotalSpace(getSysSettingDto().getUserInitialSpace() * 1024 * 1024L);
        }
        saveUserUsedSpace(userId, userSpaceDto);
        return userSpaceDto;
    }

    public UserSpaceDto getUserUsedSpace(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) searchAndUseInRedis.opsForValue().get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (userSpaceDto == null|| userSpaceDto.getUseSpace() == null || userSpaceDto.getTotalSpace() == null) {
            userSpaceDto = resetUserUsedSpace(userId);
        }
        return userSpaceDto;
    }

    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId,
                currentSize+fileSize, Constants.REDIS_TEMP_EXPIRE_TIME_HOUR, TimeUnit.HOURS);
    }

    // Get the temp id
    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }


    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = searchAndUseInRedis.opsForValue().get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }
        return 0L;
    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.FIVE, TimeUnit.MINUTES);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) searchAndUseInRedis.opsForValue().get(Constants.REDIS_KEY_DOWNLOAD + code);
    }



}
