package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.Result;
import com.exam.dto.LoginRequest;
import com.exam.dto.LoginResponse;
import com.exam.dto.RegisterRequest;
import com.exam.entity.User;
import com.exam.mapper.UserMapper;
import com.exam.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理")
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthController(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, req.getUsername())
                        .eq(User::getPassword, req.getPassword()));
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return Result.success(new LoginResponse(token, user.getId(),
                user.getUsername(), user.getRealName()));
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public Result<LoginResponse> register(@RequestBody RegisterRequest req) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (existing != null) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setRealName(req.getRealName());
        user.setRole("user");
        userMapper.insert(user);
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        return Result.success(new LoginResponse(token, user.getId(),
                user.getUsername(), user.getRealName()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<LoginResponse> me(@RequestHeader("Authorization") String auth) {
        if (auth == null || !jwtUtil.validate(auth)) {
            return Result.error(401, "未登录");
        }
        Long userId = jwtUtil.getUserId(auth);
        String username = jwtUtil.getUsername(auth);
        User user = userMapper.selectById(userId);
        return Result.success(new LoginResponse(null, userId, username,
                user != null ? user.getRealName() : ""));
    }
}
