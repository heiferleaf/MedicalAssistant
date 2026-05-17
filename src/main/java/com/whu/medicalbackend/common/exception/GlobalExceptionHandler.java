package com.whu.medicalbackend.common.exception;

import com.whu.medicalbackend.common.response.Result;
import com.whu.medicalbackend.common.response.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 这里返回一个Result<Void>本质是编译器通过插入强制类型转换实现的
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(ResultCode.BUSINESS_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid触发）
     * Java知识点：Stream API处理集合
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的字段错误信息
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCode.VALIDATE_ERROR.getCode(), message);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        logger.error("未处理异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR, e.getMessage());
    }
}
