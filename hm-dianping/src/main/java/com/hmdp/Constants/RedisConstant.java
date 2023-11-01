package com.hmdp.Constants;

/**
 * @author imbzz
 * @Date 2023/9/24 22:44
 */
public class RedisConstant {
    public static final String USER_LOGIN_CODE = "hmdp:login:code:";

    public static final String USER_LOGIN_TOKEN = "login:token:";

    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String SECKILL_STOCK_KEY = " seckill:stock:";

    public static final String BLOG_LIKED_KEY = "blog:liked:";

    public static final String FEED_KEY = "blog:feed:";

    public static final String SHOP_GEO_KEY ="shop:geo:";

    public static final String USER_SIGN_KEY ="user:sign:";
    public static final Long LOGIN_CODE_TTL = 2L;

    public static final Long LONG_TOKEN_TTL = 30L;
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final Long CACHE_NULL_TTL = 2L;
}
