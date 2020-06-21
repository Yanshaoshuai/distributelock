package com.yan.distributelock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;

/**
 * @Author YSS
 * @Date 2020/6/21 21:43
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DistributeLockApp.class})
public class DistributeLockTest {

    @Resource
    RestTemplate restTemplate;
    @Test
    public void set(){
        Integer integer = restTemplate.postForObject("http://localhost:8080/distribute/lock/set?value=0", null, Integer.class);
        System.out.println(integer);
    }
    @Test
    public void get(){
        Integer integer = restTemplate.postForObject("http://localhost:8080/distribute/lock/get", null, Integer.class);
        System.out.println(integer);
    }
    @Test
    public void add(){
        Integer integer = restTemplate.postForObject("http://localhost:8080/distribute/lock/add?value=2", null, Integer.class);
        System.out.println(integer);
    }
    @Test
    public void delete(){
        Boolean flag = restTemplate.postForObject("http://localhost:8080/distribute/lock/delete?key=key1", null, Boolean.class);
        System.out.println(flag);
    }
    @Test
    public void currentChange() throws InterruptedException {
        Runnable target=()->{
            for(int i=0;i<1000;i++){
              //  restTemplate.postForObject("http://localhost:8080/distribute/lock/add?value=1", null, Integer.class);
              //  restTemplate.postForObject("http://localhost:8080/distribute/lock/addWithRedisLock?value=1", null, Integer.class);
                restTemplate.postForObject("http://localhost:8080/distribute/lock/addWithZkLock?value=1", null, Integer.class);
            }
        };
        Thread thread1=new Thread(target);
        Thread thread2=new Thread(target);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        Integer result = restTemplate.postForObject("http://localhost:8080/distribute/lock/get", null, Integer.class);
        System.out.println(result);
    }
}
