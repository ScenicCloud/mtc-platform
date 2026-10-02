package com.mtc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.entity.SysUser;
import com.mtc.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DemoUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DemoUserInitializer(SysUserMapper userMapper, BCryptPasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Value("${mtc.demo.enabled:false}")
    private boolean demoEnabled;

    @Value("${mtc.demo.username:admin}")
    private String demoUsername;

    @Value("${mtc.demo.password:admin123}")
    private String demoPassword;

    @Override
    public void run(String... args) {
        if (!demoEnabled) {
            log.info("演示用户开关已关闭，跳过创建");
            return;
        }

        try {
            String rolesJson = objectMapper.writeValueAsString(Collections.singletonList("ADMIN"));

            SysUser existing = userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, demoUsername)
            );

            if (existing != null) {
                log.info("演示用户已存在: {}", demoUsername);
                return;
            }

            SysUser user = new SysUser();
            user.setUsername(demoUsername);
            user.setPassword(passwordEncoder.encode(demoPassword));
            user.setNickname("演示管理员");
            user.setRoles(rolesJson);
            user.setEnabled(true);
            userMapper.insert(user);
            log.info("演示用户创建成功: {}", demoUsername);

        } catch (Exception e) {
            log.error("创建演示用户失败", e);
        }
    }
}
