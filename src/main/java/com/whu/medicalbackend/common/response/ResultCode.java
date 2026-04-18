package com.whu.medicalbackend.common.response;

/**
 * 枚举类 ResultCode
 * 对 HTTP 响应的状态码进行说明
 */
public enum ResultCode{
    SUCCESS(200, "操作成功"),
    BUSINESS_ERROR(400, "业务出错"),
    VALIDATE_ERROR(401, "参数校验出错"),
    NEED_REFRESH_ERROR(402, "需要重新获取accessToken"),
    NEED_LOGIN_ERROR(403, "需要重新登录"),
    SYSTEM_ERROR(500, "系统出错");

    private ResultCode(int c, String m) {
        this.code = c;
        this.message = m;
    }

    private final int code;
    private final String message;

    public int    getCode()    { return this.code; }
    public String getMessage() { return this.message; }
}
