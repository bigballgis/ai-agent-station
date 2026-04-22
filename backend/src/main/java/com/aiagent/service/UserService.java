package com.aiagent.service;

import com.aiagent.common.ResultCode;
import com.aiagent.entity.User;
import com.aiagent.exception.BusinessException;
import com.aiagent.repository.UserRepository;
import com.aiagent.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return userRepository.findByTenantId(tenantId);
        }
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
    }

    public User getUserByUsername(String username) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return userRepository.findByUsernameAndTenantId(username, tenantId)
                    .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
    }

    @Transactional
    public User createUser(User user) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        if (tenantId != null) {
            user.setTenantId(tenantId);
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new BusinessException(ResultCode.RESOURCE_ALREADY_EXISTS.getCode(), "用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = getUserById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
