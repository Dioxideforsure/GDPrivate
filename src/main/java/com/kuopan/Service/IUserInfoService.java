package com.kuopan.Service;

import com.kuopan.Entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuopan.Entity.dto.SessionWebUserDto;

/**
 * <p>
 * To put in User Infomation 服务类
 * </p>
 *
 * @author Kuo
 * @since 2026-01-13
 */
public interface IUserInfoService extends IService<UserInfo> {

    // Login method
    SessionWebUserDto login(String email, String password);

    // UserId to search
    UserInfo selectByUserId(String userId);

    // Reset password
    void resetPassword(String email, String password, String emailCode);

    // Update password
    void updatePassword(SessionWebUserDto userDto, String oldPassword, String password );

    void updateUserStatus(String userId, Boolean status);

    void changeUserSpace(String userId, Integer changeSpace);
}
