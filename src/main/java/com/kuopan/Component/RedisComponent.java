package com.kuopan.Component;

import com.kuopan.DAO.FileInfoMapper;
import com.kuopan.Entity.constants.Constants;
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

    public SysSettingsDto getSysSettingDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) searchAndUseInRedis.opsForValue().get(Constants.REDIS_KEY_SYS_SETTINGS);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_SYS_SETTINGS, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    public void saveUserUsedSpace(String userId, UserSpaceDto userSpaceDto) {
        searchAndUseInRedis.opsForValue().set(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.SEVEN, TimeUnit.DAYS);
    }

    public UserSpaceDto getUserUsedSpace(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) searchAndUseInRedis.opsForValue().get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (userSpaceDto == null) {
            userSpaceDto = new UserSpaceDto();
            userSpaceDto.setUseSpace(fileInfoMapper.selectUsedSpace(userId));
            userSpaceDto.setTotalSpace(getSysSettingDto().getUserInitialSpace() * Constants.MB);
            saveUserUsedSpace(userId, userSpaceDto);
        }
        return userSpaceDto;
    }

}
