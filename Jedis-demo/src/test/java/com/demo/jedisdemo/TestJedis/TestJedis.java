package com.demo.jedisdemo.TestJedis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

/**
 * @author imbzz
 * @Date 2023/9/23 18:06
 */
@SpringBootTest
public class TestJedis {

    private Jedis jedis;

    @BeforeEach
    public void setUp(){
        //连接
        jedis = new Jedis("127.0.0.1", 6379);
        //设置密码
        jedis.select(0);
    }

    @Test
    public void testString(){
        jedis.set("name","imbzz");
        System.out.println(jedis.get("name"));
    }

    @AfterAll
    public void testDowm(){
        if(jedis != null){
            jedis.close();
        }
    }
}
