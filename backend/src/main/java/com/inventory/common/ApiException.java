package com.inventory.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常封装类。
 *
 * <p>Service 层通过抛出该异常表达可预期错误，例如参数非法、
 * 数据不存在、重复编号、库存不足等，统一交给全局异常处理器返回 JSON。</p>
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 400：请求参数或业务输入不合法。 */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 404：目标数据不存在。 */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** 409：数据冲突，例如编号重复或库存不足。 */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /** 403：当前用户权限不足。 */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }
}
