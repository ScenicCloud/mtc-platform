package com.mtc.common;

public enum ErrorCode {

    SUCCESS(0, "OK"),

    // 1xxx 参数错误
    PARAM_MISSING(1001, "参数缺失"),
    PARAM_INVALID(1002, "参数格式错误"),
    PARAM_OUT_OF_RANGE(1003, "参数超出允许范围"),
    NOT_FOUND(1004, "接口不存在"),
    METHOD_NOT_ALLOWED(1005, "请求方法不支持"),
    UNSUPPORTED_MEDIA_TYPE(1006, "请求内容类型不支持"),

    // 2xxx 认证授权
    UNAUTHORIZED(2001, "未登录或登录已失效"),
    TOKEN_EXPIRED(2002, "登录已过期"),
    FORBIDDEN(2003, "无权限"),
    BAD_CREDENTIALS(2004, "账号或密码错误"),

    // 5xxx 服务端错误
    DEPENDENCY_UNAVAILABLE(5001, "依赖服务不可用"),
    DATABASE_ERROR(5002, "数据库操作失败"),
    CACHE_ERROR(5003, "缓存操作失败"),
    INTERNAL_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
