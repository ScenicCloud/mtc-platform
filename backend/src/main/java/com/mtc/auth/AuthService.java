package com.mtc.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.auth.dto.LoginResponse;
import com.mtc.auth.dto.UserInfo;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.entity.SysUser;
import com.mtc.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public AuthService(SysUserMapper userMapper, JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    public LoginResponse login(String username, String password) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        // 用户不存在或密码错误，统一返回相同错误（防撞库）
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "账号已被禁用");
        }

        String token = jwtUtil.generate(user.getId(), user.getUsername());
        List<String> roles = parseRoles(user.getRoles());
        UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), roles);

        return new LoginResponse(token, "Bearer", jwtUtil.getExpireSeconds(), userInfo);
    }

    public UserInfo getCurrentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<String> roles = parseRoles(user.getRoles());
        return new UserInfo(user.getId(), user.getUsername(), roles);
    }

    private List<String> parseRoles(String rolesJson) {
        if (rolesJson == null || rolesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析用户角色失败: {}", rolesJson);
            return Collections.emptyList();
        }
    }
}
