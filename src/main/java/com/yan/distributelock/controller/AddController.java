package com.yan.distributelock.controller;
import com.yan.distributelock.zk.ZookeeperDistributeLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author YSS
 * @Date 2020/6/21 21:00
 */
@RestController
public class AddController {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ZookeeperDistributeLock zkLock;
    @RequestMapping("/addWithRedisLock")
    public String addWithRedisLock(Integer value){
        RLock addLock = redissonClient.getLock("addLock");
        addLock.lock();
            Object key1 = redisTemplate.opsForValue().get("sum");
            key1=(int)key1+value;
            redisTemplate.opsForValue().set("sum",key1);
        addLock.unlock();
        return key1.toString();
    }
    @RequestMapping("/addWithZkLock")
    public String addWithZkLock(Integer value){
        zkLock.getLock();
            Object key1 = redisTemplate.opsForValue().get("sum");
            key1=(int)key1+value;
            redisTemplate.opsForValue().set("sum",key1);
        zkLock.releaseLock();
        return key1.toString();
    }
    @RequestMapping("/add")
    public String add(Integer value){
        Object key1 = redisTemplate.opsForValue().get("sum");
        key1=(int)key1+value;
        redisTemplate.opsForValue().set("sum",key1);
        return key1.toString();
    }
    @RequestMapping("/set")
    public String set(Integer value){
        redisTemplate.opsForValue().set("sum",value);
        Object key1 = redisTemplate.opsForValue().get("sum");
        return key1.toString();
    }
    @RequestMapping("/get")
    public String get(){
        Object key1 = redisTemplate.opsForValue().get("sum");
        return key1.toString();
    }
    @RequestMapping("/delete")
    public String delete(String key){
        Object key1 = redisTemplate.delete(key);
        return key1.toString();
    }
}
