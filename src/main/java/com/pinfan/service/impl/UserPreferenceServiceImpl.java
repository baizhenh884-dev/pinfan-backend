package com.pinfan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.entity.UserPreference;
import com.pinfan.mapper.UserPreferenceMapper;
import com.pinfan.service.UserPreferenceService;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceServiceImpl extends ServiceImpl<UserPreferenceMapper, UserPreference> implements UserPreferenceService {
}
