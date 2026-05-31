package com.whu.medicalbackend.common.client;

<<<<<<< HEAD
import org.springframework.stereotype.Service;

@Service
public class FamilyServiceClient {
    
    public Long getGroupIdByUserId(Long userId) {
        return 1L;
    }
    
    public void insertEventLog(Long groupId, Long userId, String eventType, String medicineName) {
        // mock implementation
=======
import com.whu.medicalbackend.common.client.dto.FamilyMemberDTO;
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
 * 家庭服务客户端
 *
 * 用于调用 FamilyService 的内部 API
 *
 * @author Medical Assistant Team
 */
@Component
public class FamilyServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(FamilyServiceClient.class);

    @Autowired
    @Qualifier("interServiceRestTemplate")
    private RestTemplate restTemplate;

    @Value("${service.family.url:http://family-service:8083}")
    private String familyServiceUrl;

    /**
     * 根据用户 ID 查询家庭成员信息
     *
     * @param userId 用户 ID
     * @return 家庭成员信息
     */
    public FamilyMemberDTO getMemberByUserId(Long userId) {
        String url = familyServiceUrl + "/internal/family/member/by-user/" + userId;
        logger.debug("调用 FamilyService: {}", url);

        try {
            ResponseEntity<Result<FamilyMemberDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<FamilyMemberDTO>>() {}
            );

            Result<FamilyMemberDTO> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 FamilyService 失败: {}", result != null ? result.getMessage() : "unknown");
            return null;
        } catch (Exception e) {
            logger.error("调用 FamilyService 异常: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 查询家庭组的所有活跃成员用户 ID
     *
     * @param groupId 家庭组 ID
     * @return 活跃成员用户 ID 列表
     */
    public List<Long> getActiveMembers(Long groupId) {
        String url = familyServiceUrl + "/internal/family/group/" + groupId + "/active-members";
        logger.debug("调用 FamilyService 查询活跃成员: {}", url);

        try {
            ResponseEntity<Result<List<Long>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<List<Long>>>() {}
            );

            Result<List<Long>> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 FamilyService 查询活跃成员失败: {}", result != null ? result.getMessage() : "unknown");
            return List.of();
        } catch (Exception e) {
            logger.error("调用 FamilyService 查询活跃成员异常: groupId={}", groupId, e);
            return List.of();
        }
    }

    /**
     * 检查用户是否在家庭组中
     *
     * @param userId 用户 ID
     * @return 是否在家庭组中
     */
    public boolean checkUserInGroup(Long userId) {
        String url = familyServiceUrl + "/internal/family/member/" + userId + "/in-group";
        logger.debug("调用 FamilyService 检查用户是否在家庭组: {}", url);

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
            logger.error("调用 FamilyService 检查用户是否在家庭组异常: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 获取用户所在的家庭组 ID
     *
     * @param userId 用户 ID
     * @return 家庭组 ID
     */
    public Long getGroupIdByUserId(Long userId) {
        String url = familyServiceUrl + "/internal/family/member/" + userId + "/group-id";
        try {
            ResponseEntity<Result<Long>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<Long>>() {});
            Result<Long> result = response.getBody();
            return (result != null && result.getCode() == 200) ? result.getData() : null;
        } catch (Exception e) {
            logger.error("调用 FamilyService 获取家庭组ID异常: userId={}", userId, e);
            return null;
        }
    }

    public void insertEventLog(Long groupId, Long userId, String eventType, String eventContent) {
        String url = familyServiceUrl + "/internal/family/event-log?groupId=" + groupId
                + "&userId=" + userId + "&eventType=" + eventType + "&eventContent=" + eventContent;
        try {
            restTemplate.postForEntity(url, null, Void.class);
        } catch (Exception e) {
            logger.error("调用 FamilyService 写入事件日志异常: groupId={}, userId={}", groupId, userId, e);
        }
>>>>>>> fc315ac1946580259e7f744c73e93a43411327db
    }
}
