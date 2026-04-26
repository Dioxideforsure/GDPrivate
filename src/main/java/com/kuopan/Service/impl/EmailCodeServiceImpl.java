package com.kuopan.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kuopan.Component.RedisComponent;
import com.kuopan.Config.AppConfig;
import com.kuopan.DAO.UserInfoMapper;
import com.kuopan.Entity.UserInfo;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.SysSettingsDto;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.EmailCodeService;
import com.kuopan.Util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailCodeServiceImpl implements EmailCodeService {
    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Async
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer Type) {
        if (!Type.equals(1)) {
            new BusinessException("有人捣乱，调用了其它接口");
        }
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getEmail, email);
        UserInfo user = userInfoMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("邮箱未注册，请联系你的管理员或者导师");
        }

        String code = StringUtil.getRandomNumber(Constants.LENGTH_5);
        // Send Verification code
        sendEmailCode(email, code);

        String redisKey = Constants.REDIS_KEY_PREFIX_EMAIL + email;
        if (stringRedisTemplate.hasKey(redisKey)) {
            stringRedisTemplate.delete(redisKey);
        } // Delete previous email code if click for second time.
        stringRedisTemplate.opsForValue().set(redisKey, code, Constants.EIGHT, TimeUnit.MINUTES);
    }

    private void sendEmailCode(String toEmail, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(appConfig.getSendUserName());
            helper.setTo(toEmail);

            SysSettingsDto sysSettingsDto = redisComponent.getSysSettingDto();

            helper.setSubject(sysSettingsDto.getRegisterEmailTitle());
            helper.setText(String.format(sysSettingsDto.getRegisterEmailContent(), code));
            helper.setSentDate(new Date());

            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new BusinessException("邮件发送失败");
        }
    }

    @Override
    public void checkCode(String email, String code) {
        String key = Constants.REDIS_KEY_PREFIX_EMAIL + email;
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new BusinessException("邮箱验证码已失效，请重新获取");
        }

        if (!code.equals(value)) {
            throw new BusinessException("邮箱验证码不正确");
        }

        stringRedisTemplate.delete(key);
    }
}
