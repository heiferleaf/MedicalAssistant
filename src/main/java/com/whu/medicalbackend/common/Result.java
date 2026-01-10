package com.whu.medicalbackend.common;

/**
 * 返回结果的包装类 Result
 * 实例的创建交给工厂
 * @param <T>：使用参数化类型，来对各种类型的数据统一处理
 */
public class Result<T> {
    private int     code;
    private String  message;
    private T       data;   // 其中data可以是null

    // 设置构造函数私有，强制通过工厂创建
    private Result() {}
    private Result(int code, String message, T data) {
        this.code       = code;
        this.message    = message;
        this.data       = data;
    }

    // ========= 静态工厂创建 ==========
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<T>(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<T>(ResultCode.SUCCESS. getCode(), message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<T>(code, message, null);
    }

    /**
     * 失败响应（使用枚举）
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<T>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<T>(resultCode.getCode(), message, null);
    }

    // ============ Getter/Setter ============
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
