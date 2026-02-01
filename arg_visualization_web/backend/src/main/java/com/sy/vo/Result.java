package com.sy.vo;

import com.sy.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 * 适配 vue-vben-admin 前端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;   // 响应码，0 代表成功; 非0 代表失败（适配 vben-admin）
    private String message; // 响应信息
    private T data;         // 返回的数据

    //增删改 成功响应
    public static <T> Result<T> success() {
        return new Result<T>(0, I18nUtil.getMessage("success"), null);
    }
    
    //查询 成功响应
    public static <T> Result<T> success(T data) {
        return new Result<T>(0, I18nUtil.getMessage("success"), data);
    }
    
    //查询 成功响应（自定义消息）
    public static <T> Result<T> success(String message, T data) {
        return new Result<T>(0, message, data);
    }
    
    //查询 成功响应（使用消息码）
    public static <T> Result<T> successWithCode(String messageCode, T data) {
        return new Result<T>(0, I18nUtil.getMessage(messageCode), data);
    }
    
    //查询 成功响应（使用消息码，带参数）
    public static <T> Result<T> successWithCode(String messageCode, Object[] args, T data) {
        return new Result<T>(0, I18nUtil.getMessage(messageCode, args), data);
    }
    
    //失败响应
    public static <T> Result<T> error(String message) {
        return new Result<T>(500, message, null);
    }
    
    //失败响应（使用消息码）
    public static <T> Result<T> errorWithCode(String messageCode) {
        return new Result<T>(500, I18nUtil.getMessage(messageCode), null);
    }
    
    //失败响应（使用消息码，带参数）
    public static <T> Result<T> errorWithCode(String messageCode, Object[] args) {
        return new Result<T>(500, I18nUtil.getMessage(messageCode, args), null);
    }
    
    //失败响应（自定义错误码）
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>(code, message, null);
    }
    
    //失败响应（自定义错误码，使用消息码）
    public static <T> Result<T> errorWithCode(Integer code, String messageCode) {
        return new Result<T>(code, I18nUtil.getMessage(messageCode), null);
    }
}
