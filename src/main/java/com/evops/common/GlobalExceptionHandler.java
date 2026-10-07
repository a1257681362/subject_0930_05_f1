package com.evops.common;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(ex.getMessage());
    }

    /** Bean Validation 校验失败：返回第一个字段的错误提示 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数校验失败";
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception ex) {
        return ApiResponse.fail("系统处理失败");
    }
}
