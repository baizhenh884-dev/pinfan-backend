package com.pinfan.controller;

import cn.hutool.core.util.StrUtil;
import com.pinfan.common.result.R;
import com.pinfan.dto.LoginDTO;
import com.pinfan.dto.PhoneDTO;
import com.pinfan.dto.UpdateMeDTO;
import com.pinfan.dto.UserVO;
import com.pinfan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/code")
    @Operation(summary = "发送验证码")
    public R<Void> sendCode(@RequestBody @Valid PhoneDTO phoneDTO) {
        userService.sendCode(phoneDTO.getPhone());
        return R.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "验证码登录（首次登录自动注册）")
    public R<String> userLogin(@RequestBody @Valid LoginDTO loginDTO) {
        String token = userService.userLogin(loginDTO.getPhone(), loginDTO.getCode());
        return R.ok("登录成功", token);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<UserVO> getMe() {
        return R.ok(userService.getMe());
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户昵称/头像")
    public R<UserVO> updateMe(@RequestBody @Valid UpdateMeDTO dto,
                              HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        return R.ok(userService.updateMe(dto, token));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(token)) {
            userService.logout(token);
        }
        return R.ok("已退出", null);
    }
}
