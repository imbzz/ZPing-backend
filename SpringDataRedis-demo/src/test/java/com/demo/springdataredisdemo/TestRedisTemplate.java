package com.demo.springdataredisdemo;

import com.demo.springdataredisdemo.pojo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ScriptOutputType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author imbzz
 * @Date 2023/9/23 20:34
 */
@SpringBootTest
public class TestRedisTemplate {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void TestRedis(){
        redisTemplate.opsForValue().set("name","jack");
        String name = (String)redisTemplate.opsForValue().get("name");
        System.out.println(name);
    }

    @Test
    public void TestRedisObjecct(){
        redisTemplate.opsForValue().set("user:1",new User("imbzz",12));
        User o = (User) redisTemplate.opsForValue().get("user:1");
        System.out.println(o);
    }

    @Test
    public void TestStringRedisTemplate() throws JsonProcessingException {
        User user = new User("jack", 13);
        String userString = mapper.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set("user:2",userString);
        String s = stringRedisTemplate.opsForValue().get("user:2");
        User user1 = mapper.readValue(s, User.class);
        System.out.println(user1);

    }
}
