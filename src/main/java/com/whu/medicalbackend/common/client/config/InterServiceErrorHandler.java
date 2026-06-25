package com.whu.medicalbackend.common.client.config;

import com.whu.medicalbackend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * 服务间调用错误处理器
 *
 * 将 HTTP 错误转换为业务异常
 *
 * @author Medical Assistant Team
 */
public class InterServiceErrorHandler implements ResponseErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(InterServiceErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatus.Series series = HttpStatus.Series.resolve(response.getStatusCode().value());
        return series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        String statusText = response.getStatusText();

        logger.error("服务间调用失败: status={}, text={}", statusCode, statusText);

        if (statusCode == HttpStatus.NOT_FOUND) {
            throw new BusinessException("目标服务资源不存在");
        } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new BusinessException("目标服务暂时不可用");
        } else if (statusCode != null && statusCode.is5xxServerError()) {
            throw new BusinessException("目标服务内部错误");
        } else {
            throw new BusinessException("服务间调用失败: " + statusText);
        }
    }
}
