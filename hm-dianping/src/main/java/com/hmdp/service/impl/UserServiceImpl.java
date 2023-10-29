package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.Constants.RedisConstant.*;
import static com.hmdp.Constants.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {



    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.验证手机号参数
        if(!RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机格式错误");
        }
        //2.生成code
        String code = RandomUtil.randomNumbers(6);
        //3.存入redis
        stringRedisTemplate.opsForValue().set(USER_LOGIN_CODE+phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //4.发送消息
        log.debug("发送验证码成功, 验证码为:{" + code+"}");
        //5.返回成功
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.验证参数手机号
        String phone = loginForm.getPhone();
        //2.不符合返回错误
        if(StringUtils.isBlank(phone)){
            return Result.fail("手机号格式错误");
        }
        //3.从redis获取验证码
        String code = loginForm.getCode();
        String cacheCode = stringRedisTemplate.opsForValue().get(USER_LOGIN_CODE + phone);
        if(cacheCode == null || !cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }
        //4.数据库查询用户
        User user = query().eq("phone", phone).one();
        //5.判断用户是否存在
        if (user == null){
            //6.不存在注册用户
           user =  createUserWithPhone(phone);
        }
        //todo 7.存在则保存用户到redis，
        //todo 7.1随机生成token
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //todo 7.3将user变成map
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue)-> fieldValue.toString()));
        //todo 7.2将token对象hash存在redis
        String key = USER_LOGIN_TOKEN + token;
        stringRedisTemplate.opsForHash().putAll(key,userMap);
        //todo 7.3设置过期时间
        stringRedisTemplate.expire(key,30,TimeUnit.MINUTES);
        //8.返回token给前端
        return Result.ok(token);
    }

    /**
     * 创建用户
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
