package com.inventory.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 全局异常处理器。
 *
 * <p>将参数校验、登录失败、权限不足、上传大小超限、业务异常等情况
 * 统一转换为前端可直接展示的 JSON 错误信息，保证系统具备基本容错能力。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 处理 Service 层主动抛出的业务异常。 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of("message", error.getMessage()));
    }

    /** 处理 @Valid 和约束校验失败。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> handleValidation(Exception error) {
        String message = "参数校验失败";
        if (error instanceof MethodArgumentNotValidException validError
                && validError.getBindingResult().getFieldError() != null) {
            message = validError.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    /** 处理用户名或密码错误。 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误"));
    }

    /** 处理数据库唯一约束、外键关联等数据冲突。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "数据重复或被关联引用，操作已拦截"));
    }

    /** 处理图片上传大小超过配置限制。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadSize() {
        return ResponseEntity.badRequest().body(Map.of("message", "图片大小不能超过 3MB"));
    }

    /** 处理不存在的接口或静态资源。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "接口或资源不存在"));
    }

    /** 兜底处理未知异常，避免后端直接返回堆栈到前端。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception error) {
        error.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "服务器内部错误"));
    }
}
