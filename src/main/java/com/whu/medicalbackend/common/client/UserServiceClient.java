package com.whu.medicalbackend.common.client;

import com.whu.medicalbackend.common.client.dto.UserDTO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceClient {
    
    public UserDTO getUserById(Long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setUsername("test_user_" + userId);
        return user;
    }
    
    public UserDTO getUserByPhone(String phone) {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUsername("test_user");
        return user;
    }
    
    public String getNickname(Long userId) {
        return "test_user_" + userId;
    }
}
