package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.MedicationTaskDTO;
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

import java.util.List;

/**
 * 服药服务客户端
 *
 * 用于调用 MedicationService 的内部 API
 *
 * @author Medical Assistant Team
 */
@Component
public class MedicationServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MedicationServiceClient.class);

    @Autowired
    @Qualifier("interServiceRestTemplate")
    private RestTemplate restTemplate;

    @Value("${service.medication.url:http://medication-service:8082}")
    private String medicationServiceUrl;

    /**
     * 根据任务 ID 查询服药任务
     *
     * @param taskId 任务 ID
     * @return 服药任务信息
     */
    public MedicationTaskDTO getTaskById(Long taskId) {
        String url = medicationServiceUrl + "/internal/medication/task/" + taskId;
        logger.debug("调用 MedicationService: {}", url);

        try {
            ResponseEntity<Result<MedicationTaskDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<MedicationTaskDTO>>() {}
            );

            Result<MedicationTaskDTO> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 MedicationService 失败: {}", result != null ? result.getMessage() : "unknown");
            return null;
        } catch (Exception e) {
            logger.error("调用 MedicationService 异常: taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 查询用户指定日期的服药任务列表
     *
     * @param userId 用户 ID
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 服药任务列表
     */
    public List<MedicationTaskDTO> getTasksByUserAndDate(Long userId, String date) {
        String url = medicationServiceUrl + "/internal/medication/task/user/" + userId + "/date/" + date;
        logger.debug("调用 MedicationService 查询用户任务: {}", url);

        try {
            ResponseEntity<Result<List<MedicationTaskDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<List<MedicationTaskDTO>>>() {}
            );

            Result<List<MedicationTaskDTO>> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 MedicationService 查询用户任务失败: {}", result != null ? result.getMessage() : "unknown");
            return List.of();
        } catch (Exception e) {
            logger.error("调用 MedicationService 查询用户任务异常: userId={}, date={}", userId, date, e);
            return List.of();
        }
    }

    public int countTasksByDate(Long userId, String date, Integer status) {
        String url = medicationServiceUrl + "/internal/medication/task/user/" + userId + "/count?date=" + date
                + (status != null ? "&status=" + status : "");
        try {
            ResponseEntity<Result<Integer>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<Integer>>() {});
            Result<Integer> result = response.getBody();
            return (result != null && result.getCode() == 200 && result.getData() != null) ? result.getData() : 0;
        } catch (Exception e) {
            logger.error("调用 MedicationService 计数异常: userId={}, date={}", userId, date, e);
            return 0;
        }
    }
}
