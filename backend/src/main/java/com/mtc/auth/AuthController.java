package com.mtc.auth;

import com.mtc.auth.dto.LoginRequest;
import com.mtc.auth.dto.LoginResponse;
import com.mtc.auth.dto.UserInfo;
import com.mtc.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证", description = "登录、获取当前用户信息")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(
                request.getUsername(), request.getPassword());
        return Result.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfo> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserInfo user = authService.getCurrentUser(userId);
        return Result.ok(user);
    }
}
