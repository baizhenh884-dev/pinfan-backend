package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.dto.UpdateMeDTO;
import com.pinfan.dto.UserVO;
import com.pinfan.entity.User;

public interface UserService extends IService<User> {
    void sendCode(String phone);

    String userLogin(String phone, String code);

    UserVO getMe();

    UserVO updateMe(UpdateMeDTO dto, String token);

    void logout(String token);
}
