package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author imbzz
 * @Date 2023/10/24 11:41
 */
public class SimpleRedisLock implements ILock{

    private String name;
    private static final String KEY_PREFIX="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static{
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示
        String threadId = ID_PREFIX+Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId , timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    @Override
    public void unlock() {
        //调用lua代码
        stringRedisTemplate.execute(UNLOCK_SCRIPT
                ,Collections.singletonList(KEY_PREFIX + name)
                ,ID_PREFIX+Thread.currentThread().getId()
                );
    }

//    @Override
//    public void unlock() {
//        //获取线程id
//        String threadId = ID_PREFIX+Thread.currentThread().getId();
//        //获取锁的标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//
//        //判断二者是否一致
//        if (threadId.equals(id)) {
//            //一致则删除锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}
