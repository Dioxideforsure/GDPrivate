package com.kuopan.Controller;

import com.kuopan.Annotation.GlobalInterceptor;
import com.kuopan.Annotation.VerifyParams;
import com.kuopan.Component.RedisComponent;
import com.kuopan.Entity.constants.Constants;
import com.kuopan.Entity.dto.CreateImageCode;
import com.kuopan.Entity.dto.SessionWebUserDto;
import com.kuopan.Entity.dto.UserSpaceDto;
import com.kuopan.Entity.enums.VerifyRegexEnum;
import com.kuopan.Exception.BusinessException;
import com.kuopan.Service.impl.EmailCodeServiceImpl;
import com.kuopan.Service.impl.UserInfoServiceImpl;
import com.kuopan.Util.SHAUtil;
import com.kuopan.vo.ResponseVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * <p>
 * To put in User Infomation 前端控制器
 * </p>
 *
 * @author Kuo
 * @since 2026-01-13
 */
@RestController("userInfoController")
public class UserInfoController extends BaseController {

    @Resource
    private UserInfoServiceImpl userInfoService;

    @Resource
    private EmailCodeServiceImpl emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    /*
     *  Generate Image Check Code
     */

    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    /*
     * Email check code
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParams(required = true) String email,
                                    @VerifyParams(required = true) String checkCode,
                                    @VerifyParams(required = true) Integer type) {
        try {

            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图像验证码错误");
            }
            emailCodeService.sendEmailCode(email, type);
            return ResponseVO.success(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    /*
     * Login
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO login(HttpSession session,
                            @VerifyParams(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                            @VerifyParams(required = true) String password,
                            @VerifyParams(required = true) String checkCode) {

        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图形验证码错误");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
            System.out.println("Notice:" + email + password);
            System.out.println("Notice:" + sessionWebUserDto.getUserId());
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return ResponseVO.success(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /*
     * Reset the password
     */
    @RequestMapping("/resetPassword")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO resetPassword(HttpSession session,
                                    @VerifyParams(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                    @VerifyParams(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                                    @VerifyParams(required = true) String checkCode,
                                    @VerifyParams(required = true) String emailCode) {

        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图形验证码错误");
            }
            userInfoService.resetPassword(email, password, emailCode);

            return ResponseVO.success(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /*
     * Get used space
     */
    @RequestMapping("/getUsedSpace")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUsedSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserSpaceDto userSpaceDto = redisComponent.getUserUsedSpace(sessionWebUserDto.getUserId());
        return ResponseVO.success(userSpaceDto);
    }

    /*
     * Logout
     */
    @RequestMapping("/exit")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO logout(HttpSession session) {
        if (session != null){
            session.invalidate();
        }
        return ResponseVO.success(null);
    }

    /**
     * Update the password in the page.
     */
    @RequestMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session,
                                     @VerifyParams(required = true) String oldPassword,
                                     @VerifyParams(required = true, regex = VerifyRegexEnum.PASSWORD) String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        userInfoService.updatePassword(sessionWebUserDto, SHAUtil.SHA256Encrypt(oldPassword), SHAUtil.SHA256Encrypt(password));
        return ResponseVO.success(null);
    }
}
