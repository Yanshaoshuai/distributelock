package com.yan.distributelock.config;
import com.yan.distributelock.zk.ZookeeperDistributeLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * @Author YSS
 * @Date 2020/6/21 22:11
 */
@Configuration
public class Configration {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private String port;
    @Value("${spring.redis.password}")
    private String password;
    @Value("${zookeeper.address}")
    private String zkAddress;
    @Value("${zookeeper.timeout}")
    private int zkTimeout;
    @Value("${zookeeper.retryTimes}")
    private int zkRetryTimes;
    @Value("${zookeeper.btTime}")
    private int zkSleepBTRetries;
    @Value("${zookeeper.namespace}")
    private String zkNamespace;
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
    @Bean
    public RedissonClient redisson(){
        Config config=new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port).setPassword(password);
        return Redisson.create(config);
    }
    @Bean
    public CuratorFramework zkClient(){
        CuratorFramework zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .connectionTimeoutMs(zkTimeout)
                .namespace(zkNamespace)
                .retryPolicy(new RetryNTimes(zkRetryTimes, zkSleepBTRetries))
                .build();
        zkClient.start();
        return zkClient;
    }
    @DependsOn("zkClient")
    @Bean
    public ZookeeperDistributeLock addLock(@Autowired CuratorFramework zkClient) throws Exception {
        ZookeeperDistributeLock addLock=new ZookeeperDistributeLock(zkClient,"lockRoot","addLock");
        addLock.init();
        return addLock;
    }
}
