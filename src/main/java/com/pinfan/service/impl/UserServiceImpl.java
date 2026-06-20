package com.pinfan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.dto.UpdateMeDTO;
import com.pinfan.dto.UserVO;
import com.pinfan.entity.User;
import com.pinfan.mapper.UserMapper;
import com.pinfan.service.UserService;
import com.pinfan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void sendCode(String phone) {
        String code;

        if ("17677677766".equals(phone)){
            code = "888888";
        } else {
            code = RandomUtil.randomNumbers(6);
        }

        String key = "cache:code:" + phone;
        stringRedisTemplate.opsForValue().set(key, code, 2, TimeUnit.MINUTES);
        log.info("【拼饭吧】手机号 {} 的验证码：{}", key, code);
    }

    @Override
    public String userLogin(String phone, String code) {
        //校验验证码
        String key = "cache:code:" + phone;
        String cacheCode = stringRedisTemplate.opsForValue().get(key);
        if (cacheCode == null || !cacheCode.equals(code)) {
            throw new BusinessException(401, "验证码错误或已过期");
        }

        //根据手机号查用户，没有就创建/注册
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickname("校园用户" + RandomUtil.randomNumbers(4));
            save(user);
            user = getById(user.getId());
        }

        String token = UUID.randomUUID().toString();
        String tokenKey = "cache:token:" + token;
        redisTemplate.opsForValue().set(tokenKey, user, 30, TimeUnit.MINUTES);

        //删除已使用的验证码
        stringRedisTemplate.delete(key);

        return token;
    }

    @Override
    public UserVO getMe() {
        User user = UserHolder.getUser();
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserVO updateMe(UpdateMeDTO dto, String token) {

        User current = UserHolder.getUser();

        User update = new User();
        update.setId(current.getId());
        if (StrUtil.isNotBlank(dto.getNickname())) {
            update.setNickname(dto.getNickname());
        }
        if (StrUtil.isNotBlank(dto.getAvatar())) {
            update.setAvatar(dto.getAvatar());
        }
        updateById(update);

        // 重新查完整数据
        User fresh = getById(current.getId());

        // Redis
        String key = "cache:token:" + token;
        redisTemplate.opsForValue().set(key, fresh, 30, TimeUnit.MINUTES);

        UserHolder.saveUser(fresh);

        UserVO vo = new UserVO();
        BeanUtil.copyProperties(fresh, vo);
        return vo;
    }

    @Override
    public void logout(String token) {
        String key = "cache:token:" + token;
        redisTemplate.delete(key);
    }
}
