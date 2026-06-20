package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.entity.User;

public interface UserService extends IService<User> {
    void sendCode(String phone);

    String userLogin(String phone, String code);
}
