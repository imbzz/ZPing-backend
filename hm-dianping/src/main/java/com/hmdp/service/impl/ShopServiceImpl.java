package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.Constants.SystemConstants;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.Constants.RedisConstant.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopById(Long id) {

        //解决缓存穿透
        //query

        //解决缓存击穿问题
        //Shop shop = queryWithMutex(id);

        //3.逻辑过期解决缓存击穿
        Shop shop = queryWithLogicalExpire(id);
        if (shop == null){
            return Result.fail("店铺不存在");
        }
        //互斥锁解决缓存击穿
        return Result.ok(shop);
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }

    /**
     * 处理缓存穿透请求方式
     * @param id
     * @return
     */
    public Shop queryWithMutex(Long id) {
        //1.从Redis查缓存
        String key = "cache:shop:"+ id;
        //2.判断是否存在
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //3.存在返回
        if(StringUtils.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //4.如果命中判断是否为null
        if(shopJson != null){
            return null;
        }

        //4.缓存重建——因为缓存击穿就是缓存本来有的，但是突然失效了，类似被打穿了的效果
        //4.1获取互斥锁
        String lockKey = "lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否获取成功
            if(!isLock){
                //4.3失败，则休眠重试
                Thread.sleep(50);
                queryWithMutex(id);
            }
            //4.3获取锁之后 再次判断缓存存在
            shopJson = stringRedisTemplate.opsForValue().get(key);
            // 模拟重建的延迟
            Thread.sleep(200);
            //4.4存在则直接返回
            if(StringUtils.isNotBlank(shopJson)){
                return JSONUtil.toBean(shopJson, Shop.class);
            }
            //5.不存在,根据id查数据库
            shop = getById(id);
            if(shop == null){
                //暴力法处理缓存击穿
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            }
            //6.插入数据
            String json = JSONUtil.toJsonStr(shop);
            stringRedisTemplate.opsForValue().set(key, json, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放互斥锁
            unlock(lockKey);
        }

        //8.不存在，返回错
        return shop;
    }


    public Shop queryWithLogicalExpire(Long id) {
        //1.从Redis查缓存
        String key = CACHE_SHOP_KEY + id;
        //2.判断是否存在
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //3.未命中存在返回
        if(StringUtils.isBlank(shopJson)){
            return null;
        }
        //4.命中，将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5.命中判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.1未过期，直接返回商城信息
            return shop;
        }
        //5.2过期，尝试获取互斥锁
        
        // 6获取互斥锁
        //6.1判断是否获取锁
        //6.2获取失败，返回商城信息
        //6.3获取成功，开启独立线程，实现缓存重建，返回商城信息


        //8.不存在，返回错
        return shop;
    }

    /**
     * 加锁
     * @param key
     * @return
     */
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }


    /**
     * 拆锁
     */
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }


    /**
     * 保存数据到redis
     */
    public void saveShop2Redis(Long id, Long expireTime){
        //1.查询数据库
        Shop shop = getById(id);
        //2.封装过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        //3.写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));

    }
}
