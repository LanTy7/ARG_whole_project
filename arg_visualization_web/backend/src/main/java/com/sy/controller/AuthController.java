package com.sy.controller;

import com.sy.vo.Result;
import com.sy.pojo.User;
import com.sy.service.LoginService;
import com.sy.util.I18nUtil;
import com.sy.util.JwtUtil;
import com.sy.vo.LoginRequest;
import com.sy.vo.LoginResponse;
import com.sy.vo.RegisterRequest;
import com.sy.vo.VerificationCodeRequest;
import com.sy.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private LoginService loginService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    public Result<String> sendVerificationCode(@RequestBody @Valid VerificationCodeRequest request) {
        try {
            loginService.sendVerificationCode(request.getEmail());
            return Result.successWithCode("auth.code.sent", null);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("邮箱格式无效") || e.getMessage().contains("invalid")) {
                return Result.errorWithCode("auth.email.invalid");
            } else if (e.getMessage().contains("已被注册") || e.getMessage().contains("exists")) {
                return Result.errorWithCode("auth.email.exists");
            }
            return Result.errorWithCode("error.server");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody @Valid RegisterRequest registerRequest) {
        try {
            loginService.register(registerRequest);
            return Result.successWithCode("auth.register.success", null);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("验证码") || e.getMessage().contains("code")) {
                return Result.errorWithCode("auth.code.invalid");
            } else if (e.getMessage().contains("邮箱") || e.getMessage().contains("email")) {
                return Result.errorWithCode("auth.email.exists");
            } else if (e.getMessage().contains("用户名") || e.getMessage().contains("username")) {
                return Result.errorWithCode("auth.username.exists");
            } else if (e.getMessage().contains("密码") || e.getMessage().contains("password")) {
                return Result.errorWithCode("auth.password.length");
            }
            return Result.errorWithCode("error.server");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            // 验证用户身份
            User user = loginService.login(loginRequest.getIdentifier(), loginRequest.getPassword(), loginRequest.getCode());
            
            // 生成Token
            String token = loginService.generateToken(user);
            
            // 记录登录日志
            loginService.recordLogin(user.getUserId());
            
            // 返回登录响应
            return Result.successWithCode("auth.login.success", new LoginResponse(token, user));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("未注册")) {
                return Result.errorWithCode("auth.email.invalid");
            } else if (e.getMessage().contains("password") || e.getMessage().contains("密码")) {
                return Result.errorWithCode("auth.login.failed");
            }
            return Result.errorWithCode("auth.login.failed");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }
    
    /**
     * 退出登录
     * @param request HTTP请求
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        try {
            // 从请求头中获取 token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // 移除 "Bearer " 前缀
                
                // 将 token 加入黑名单
                jwtUtil.addToBlacklist(token);
                
                // 记录退出日志
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    loginService.recordLogout(userId);
                }
            }
            
            return Result.successWithCode("auth.logout.success", null);
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }
    
    /**
     * 发送登录验证码（开发/测试用，直接返回验证码）
     */
    @PostMapping("/send-login-code")
    public Result<String> sendLoginCode(@RequestBody @Valid VerificationCodeRequest request) {
        try {
            String code = loginService.sendLoginCode(request.getEmail());
            return Result.success(code); // 直接返回验证码
        } catch (RuntimeException e) {
            if (e.getMessage().contains("邮箱格式无效") || e.getMessage().contains("invalid")) {
                return Result.errorWithCode("auth.email.invalid");
            } else if (e.getMessage().contains("未注册") || e.getMessage().contains("not found")) {
                return Result.errorWithCode("auth.email.invalid");
            }
            return Result.errorWithCode("error.server");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }

    /**
     * 发送重置密码验证码
     */
    @PostMapping("/send-reset-code")
    public Result<String> sendResetCode(@RequestBody @Valid VerificationCodeRequest request) {
        try {
            loginService.sendResetCode(request.getEmail());
            return Result.successWithCode("auth.code.sent", null);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("邮箱格式无效") || e.getMessage().contains("invalid")) {
                return Result.errorWithCode("auth.email.invalid");
            } else if (e.getMessage().contains("未注册") || e.getMessage().contains("not found")) {
                return Result.errorWithCode("auth.email.invalid");
            }
            return Result.errorWithCode("error.server");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody @Valid com.sy.vo.ResetPasswordRequest request) {
        try {
            loginService.resetPassword(request);
            return Result.successWithCode("auth.register.success", null);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("验证码") || e.getMessage().contains("code")) {
                return Result.errorWithCode("auth.code.invalid");
            } else if (e.getMessage().contains("邮箱") || e.getMessage().contains("email")) {
                return Result.errorWithCode("auth.email.invalid");
            } else if (e.getMessage().contains("密码") || e.getMessage().contains("password")) {
                return Result.errorWithCode("auth.password.mismatch");
            }
            return Result.errorWithCode("error.server");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }

    /**
     * 验证token有效性
     */
    @GetMapping("/verify-token")
    public Result<Void> verifyToken(HttpServletRequest request) {
        try {
            // 从请求头中获取 token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // 移除 "Bearer " 前缀
                
                // 验证token
                if (jwtUtil.validateToken(token)) {
                    return Result.success();
                }
            }
            return Result.errorWithCode("auth.token.invalid");
        } catch (Exception e) {
            return Result.errorWithCode("auth.token.invalid");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        try {
            // 从请求头中获取 token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // 移除 "Bearer " 前缀
                
                // 从token中获取用户ID
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    // 获取用户信息
                    User user = userMapper.findById(userId);
                    if (user != null) {
                        return Result.success(user);
                    }
                }
            }
            return Result.errorWithCode("auth.token.invalid");
        } catch (Exception e) {
            return Result.errorWithCode("error.server");
        }
    }
} 