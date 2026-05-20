package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.UserDTO;
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
 * 用户服务客户端
 *
 * 用于调用 UserService 的内部 API
 *
 * @author Medical Assistant Team
 */
@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    @Autowired
    @Qualifier("interServiceRestTemplate")
    private RestTemplate restTemplate;

    @Value("${service.user.url:http://user-service:8081}")
    private String userServiceUrl;

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public UserDTO getUserById(Long userId) {
        String url = userServiceUrl + "/internal/user/" + userId;
        logger.debug("调用 UserService: {}", url);

        try {
            ResponseEntity<Result<UserDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<UserDTO>>() {}
            );

            Result<UserDTO> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 UserService 失败: {}", result != null ? result.getMessage() : "unknown");
            return null;
        } catch (Exception e) {
            logger.error("调用 UserService 异常: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 批量查询用户信息
     *
     * @param userIds 用户 ID 列表
     * @return 用户信息列表
     */
    public List<UserDTO> getUsersByIds(List<Long> userIds) {
        String url = userServiceUrl + "/internal/user/batch";
        logger.debug("调用 UserService 批量查询: {}", url);

        try {
            ResponseEntity<Result<List<UserDTO>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<Result<List<UserDTO>>>() {}
            );

            Result<List<UserDTO>> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }

            logger.warn("调用 UserService 批量查询失败: {}", result != null ? result.getMessage() : "unknown");
            return List.of();
        } catch (Exception e) {
            logger.error("调用 UserService 批量查询异常: userIds={}", userIds, e);
            return List.of();
        }
    }

    /**
     * 检查用户是否存在
     *
     * @param userId 用户 ID
     * @return 是否存在
     */
    public boolean checkUserExists(Long userId) {
        String url = userServiceUrl + "/internal/user/" + userId + "/exists";
        try {
            ResponseEntity<Result<Boolean>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<Boolean>>() {});
            Result<Boolean> result = response.getBody();
            return result != null && result.getCode() == 200 && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            logger.error("调用 UserService 检查用户存在异常: userId={}", userId, e);
            return false;
        }
    }

    public UserDTO getUserByPhone(String phone) {
        String url = userServiceUrl + "/internal/user/by-phone/" + phone;
        try {
            ResponseEntity<Result<UserDTO>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Result<UserDTO>>() {});
            Result<UserDTO> result = response.getBody();
            return (result != null && result.getCode() == 200) ? result.getData() : null;
        } catch (Exception e) {
            logger.error("调用 UserService 根据手机号查询异常: phone={}", phone, e);
            return null;
        }
    }
}
