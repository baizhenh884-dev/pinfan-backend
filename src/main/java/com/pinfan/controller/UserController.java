package com.pinfan.controller;

import com.pinfan.common.result.R;
import com.pinfan.dto.LoginDTO;
import com.pinfan.dto.PhoneDTO;
import com.pinfan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
