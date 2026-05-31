package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.HealthDataDTO;
<<<<<<< HEAD
import org.springframework.stereotype.Service;

@Service
public class HealthServiceClient {
    
    public HealthDataDTO getTodayHealthData(Long userId) {
        return new HealthDataDTO();
=======
import com.whu.medicalbackend.common.response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 健康服务客户端
 *
 * 用于调用 HealthService 的内部 API
 *
 * @author Medical Assistant Team
 */
@Component
public class HealthServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(HealthServiceClient.class);

    @Autowired
    @Qualifier("interServiceRestTemplate")
    private RestTemplate restTemplate;

    @Value("${service.health.url:http://health-service:8084}")
    private String healthServiceUrl;

    /**
     * 查询用户今日的健康数据
     *
     * @param userId 用户 ID
     * @return 健康数据
     */
    public HealthDataDTO getTodayHealthData(Long userId) {
        String url = healthServiceUrl + "/internal/health/data/user/" + userId + "/today";
        logger.debug("调用 HealthService: {}", url);

        try {
            ResponseEntity<Result<HealthDataDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<HealthDataDTO>>() {}
            );

            Result<HealthDataDTO> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 HealthService 失败: {}", result != null ? result.getMessage() : "unknown");
            return null;
        } catch (Exception e) {
            logger.error("调用 HealthService 异常: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 检查用户今日是否有健康数据
     *
     * @param userId 用户 ID
     * @return 是否存在
     */
    public boolean checkTodayHealthDataExists(Long userId) {
        String url = healthServiceUrl + "/internal/health/data/user/" + userId + "/today/exists";
        logger.debug("调用 HealthService 检查今日健康数据: {}", url);

        try {
            ResponseEntity<Result<Boolean>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<Boolean>>() {}
            );

            Result<Boolean> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData() != null && result.getData();
            }

            return false;
        } catch (Exception e) {
            logger.error("调用 HealthService 检查今日健康数据异常: userId={}", userId, e);
            return false;
        }
>>>>>>> fc315ac1946580259e7f744c73e93a43411327db
    }
}
