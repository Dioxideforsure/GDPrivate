package com.kuopan.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kuopan.Component.RedisComponent;
import com.kuopan.Config.AppConfig;
import com.kuopan.DAO.FileInfoMapper;
import com.kuopan.Entity.FileInfo;
import com.kuopan.Entity.UserInfo;
import com.kuopan.DAO.UserInfoMapper;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UserSpaceDto;
import com.kuopan.Entity.enums.SHAEnum;
import com.kuopan.Entity.enums.UserStatusEnum;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.EmailCodeService;
import com.kuopan.Service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuopan.Util.SHAUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * To put in User Infomation 服务实现类
 * </p>
 *
 * @author Kuo
 * @since 2026-01-13
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Autowired
    private EmailCodeService emailCodeService;

    // Complete the login method
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionWebUserDto login(String email, String password) {
        LambdaQueryWrapper<UserInfo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserInfo::getEmail, email);
        UserInfo user = userInfoMapper.selectOne(lambdaQueryWrapper);

        if (user == null || !user.getPassword().equals(password)) {
            throw new BusinessException("账号或密码错误");
        }

        if (!user.getStatus()) {
            throw new BusinessException("账号已被禁用");
        }

        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(user.getUserId());
        sessionWebUserDto.setNickName(user.getUserName());
        sessionWebUserDto.setIsAdmin(false);
        if (ArrayUtils.contains(appConfig.getAdminEmail().split(","), email)) {
            sessionWebUserDto.setIsAdmin(true);
        }


        // User Space
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(fileInfoMapper.selectUsedSpace(user.getUserId()));
        userSpaceDto.setTotalSpace(userSpaceDto.getTotalSpace());
        redisComponent.saveUserUsedSpace(user.getUserId(), userSpaceDto);

        return sessionWebUserDto;
    }

    @Override
    public UserInfo selectByUserId(String userId) {
        return userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String password, String emailCode) {
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<>();
        query.eq(UserInfo::getEmail, email);
        UserInfo user = userInfoMapper.selectOne(query);

        if (user == null) {
            throw new BusinessException("邮箱账号不存在");
        }
        emailCodeService.checkCode(email, emailCode);

        UserInfo updateUser = new UserInfo();
        updateUser.setUserId(user.getUserId());
        updateUser.setPassword(SHAUtil.SHA256Encrypt(password));

        int rows = userInfoMapper.updateById(updateUser);

        if (rows == 0) {
            throw new BusinessException("重置密码失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(SessionWebUserDto userDto, String oldPassword, String password) {
        if (userDto == null) {
            throw new BusinessException("重置密码出现错误，登录可能过期");
        }
        String userID = userDto.getUserId();


        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<>();
        query.eq(UserInfo::getUserId, userID);
        UserInfo userInfo = userInfoMapper.selectOne(query);



        if (!userInfo.getPassword().equals(oldPassword)) {
            throw new BusinessException(403, "旧密码错误");
        }

        UserInfo updateUser = new UserInfo();
        updateUser.setUserId(userID);
        updateUser.setPassword(password);

        int rows = userInfoMapper.updateById(updateUser);
        if (rows == 0) {
            throw new BusinessException("重置密码失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(String userId, Boolean status) {
        LambdaUpdateWrapper<UserInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserInfo::getUserId, userId);
        UserInfo updateUser = new UserInfo();
        updateUser.setStatus(status);
        if (UserStatusEnum.DISABLE.getStatus().equals(status)) {
            updateUser.setOccuSpace(0L);
            fileInfoMapper.delete(new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getUserId, userId));
        }
        userInfoMapper.update(updateUser, updateWrapper);
    }

    @Override
    public void changeUserSpace(String userId, Integer changeSpace) {
        Long space = changeSpace * Constants.MB;
        UserInfo userInfo = new UserInfo();
        userInfo.setTotalSpace(space);
        userInfoMapper.update(userInfo, new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId));
        redisComponent.resetUserUsedSpace(userId);
    }
}
