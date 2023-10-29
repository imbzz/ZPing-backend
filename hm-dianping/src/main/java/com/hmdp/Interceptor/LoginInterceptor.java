package com.hmdp.Interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.Constants.RedisConstant;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author imbzz
 * @Date 2023/9/24 21:21
 */
public class LoginInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(){}
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头的token
        String token = request.getHeader("authorization");

        // 2.判断token是否为空
        if (StringUtils.isBlank(token)) {
            //3.为空返回false
            response.setStatus(401);
            return false;
        }
        //4.获取用户
        String key = RedisConstant.USER_LOGIN_TOKEN + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if(userMap.isEmpty()) {
            //4.不存在返回false
            response.setStatus(401);
            return false;
        }
        //5.将map转换为user
        UserDTO user = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //存在则存入
        UserHolder.saveUser(user);
        //刷新token
        stringRedisTemplate.expire(key,RedisConstant.LONG_TOKEN_TTL, TimeUnit.MINUTES);
        //5.放行
        return true;
    }
}
