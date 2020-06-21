package com.yan.distributelock.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * @Author YSS
 * @Date 2020/6/20 20:32
 */
public class CuratorOperator {
    private static volatile CuratorFramework client=null;
    private CuratorOperator(){
    }
    private static CuratorFramework getClient(String zkServerAddress){
        if(client!=null){
            return client;
        }else {
            synchronized (CuratorOperator.class){
                if(client==null){
                    client=CuratorFrameworkFactory.builder()
                            .connectString(zkServerAddress)
                            .sessionTimeoutMs(15000)
                            .retryPolicy(new RetryNTimes(3,5000))
                            .namespace("workspace")
                            .build();
                }
            }
        }
        return client;
    }
    public static void main(String[] args) throws InterruptedException {
        CuratorFramework client = getClient("172.16.0.3:2181");
        client.start();
        CuratorFrameworkState state = client.getState();
        System.out.println(state);
        String childNodePath="/firstlevel/secondlevel/a/b/c";
        //创建节点
        try {
            if(client.checkExists().forPath(childNodePath)==null){
                String str = client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(childNodePath, "none".getBytes());
                System.out.println(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //获取节点数据
        try {
            Stat stat=new Stat();
            byte[] bytes = client.getData()
                    .storingStatIn(stat)
                    .forPath(childNodePath);
            System.out.println(stat);
            System.out.println(client.getNamespace()+childNodePath+"----data--->"+new String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //注册watcher  usingWatcher的 CuratorWatcher Watcher 两种重载都行
        try {
            //本节点事件
            byte[] bytes1 = client.getData().usingWatcher(new CuratorWatcher() {
                @Override
                public void process(WatchedEvent watchedEvent) throws Exception {
                    System.out.println("CuratorWatcher--->"+watchedEvent);
                }
            }).forPath("/firstlevel");
          //子节点事件
           client.getChildren().usingWatcher(new CuratorWatcher() {
               @Override
               public void process(WatchedEvent watchedEvent) throws Exception {
                   System.out.println("CuratorWatcher--->"+watchedEvent);
               }
           }).forPath("/firstlevel");


        } catch (Exception e) {
            e.printStackTrace();
        }
        //修改节点数据
        try {
            Stat stat = client.setData()
//                    .withVersion(1)
                    .forPath(childNodePath,"999".getBytes());
            System.out.println(client.getNamespace()+childNodePath+"----stat--->"+stat);
            byte[] bytes = client.getData()
                    .forPath(childNodePath);
            System.out.println(client.getNamespace()+childNodePath+"----data--->"+new String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //获取所有子节点
        try {
            List<String> stringList = client.getChildren().forPath("/firstlevel");
            System.out.println(stringList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //删除节点数据
        try {
          client.delete()
                  .guaranteed()
                  .deletingChildrenIfNeeded()
//                  .withVersion(1)
                  .forPath("/firstlevel/secondlevel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //为当前节点添加监听事件
        Thread nodeCacheThread=new Thread(()->{
            //使用NodeCache注册监听  存活期间会一直监听当年前节点事件
            NodeCache nodeCache=new NodeCache(client, "/firstlevel");
            try {
                nodeCache.start(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(new String(nodeCache.getCurrentData().getData()));
            nodeCache.getListenable().addListener(new NodeCacheListener() {
                @Override
                public void nodeChanged() throws Exception {
                    System.out.println("NodeCacheListener==>"+new String(nodeCache.getCurrentData().getData()));
                }
            });
            synchronized (nodeCache){
                try {
                    nodeCache.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread childNodePathCacheThread=new Thread(()->{
            // 为子节点添加watcher
            // PathChildrenCache: 监听数据节点的增删改，会触发事件
            String childNodePathCache =  "/firstlevel";
            // cacheData: 设置缓存节点的数据状态
            final PathChildrenCache childrenCache = new PathChildrenCache(client, childNodePathCache, true);
            /**
             * StartMode: 初始化方式
             * POST_INITIALIZED_EVENT：异步初始化，初始化之后会触发事件
             * NORMAL：异步初始化
             * BUILD_INITIAL_CACHE：同步初始化
             */
            try {
                childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<ChildData> childDataList = childrenCache.getCurrentData();
            System.out.println("当前数据节点的子节点数据列表：");
            for (ChildData cd : childDataList) {
                String childData = new String(cd.getData());
                System.out.println(childData);
            }

            childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    if(event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)){
                        System.out.println("子节点初始化ok...");
                    }
                    else if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)){
                        String path = event.getData().getPath();
                        System.out.println("添加子节点:" + event.getData().getPath());
                    }else if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)){
                        System.out.println("删除子节点:" + event.getData().getPath());
                    }else if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)){
                        System.out.println("修改子节点路径:" + event.getData().getPath());
                        System.out.println("修改子节点数据:" + new String(event.getData().getData()));
                    }
                }
            });
            synchronized (childrenCache){
                try {
                    childrenCache.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        nodeCacheThread.start();
        childNodePathCacheThread.start();
        nodeCacheThread.join();
        childNodePathCacheThread.join();

        client.close();
    }
}
