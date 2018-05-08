package com.leyongzuche.commons.redis;

import redis.clients.jedis.Jedis;

/**
 * @author pengqingsong
 * @date 24/01/2018
 * @desc
 */
public class RedisLock {

    private Jedis jedis;

    public RedisLock(Jedis jedis) {
        this.jedis = jedis;
    }

    public boolean tryLock(String key) {
        Long resultCount = jedis.hset(key, "1", "1");
        if (resultCount > 0) {
            jedis.expire(key, 60);
        }
        return resultCount > 0;
    }

    public void unlock(String key) {
        jedis.hdel(key, "1");
    }

}
