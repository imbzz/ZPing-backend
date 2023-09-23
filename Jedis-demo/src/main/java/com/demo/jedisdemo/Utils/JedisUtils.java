package com.demo.jedisdemo.Utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author imbzz
 * @Date 2023/9/23 20:00
 */
public class JedisUtils {
    private static final JedisPool jedisPool;

    static{
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //最大连接
        jedisPoolConfig.setMaxTotal(8);
        //最大空闲连接
        jedisPoolConfig.setMaxIdle(8);
        //最小空闲连接
        jedisPoolConfig.setMinIdle(0);
        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(200);

        jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379, 1000);
    }

    //获取Jedis对象
    public static Jedis getJedis(){
        return jedisPool.getResource();
    }
}
