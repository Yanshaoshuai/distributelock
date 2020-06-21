package com.yan.distributelock.zk;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import java.util.concurrent.CountDownLatch;

/**
 * @Author YSS
 * @Date 2020/6/21 21:14
 */
public class ZookeeperDistributeLock {
    private CuratorFramework zkClient;
    private static CountDownLatch countDownLatch=new CountDownLatch(1);
    /**
     * 父节点
     */
    private String lockRoot;
    /**
     * zk子节点
     */
    private String lockName;
    private String pathLock;
    public ZookeeperDistributeLock(CuratorFramework zkClient,String lockRoot,String lockName) {
        this.zkClient = zkClient;
        this.lockName = lockName;
        this.lockRoot = lockRoot;
        pathLock ="/"+lockRoot+"/"+lockName;
    }
    public void init() throws Exception {
        if(zkClient.checkExists().forPath("/"+lockRoot)==null){
            zkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                    .forPath("/" + lockRoot);
        }
        //创建监听事件
        addWatcherToLockRoot("/"+lockRoot);
    }

    private void addWatcherToLockRoot(String root) throws Exception {
        PathChildrenCache cache=new PathChildrenCache(zkClient, root,true);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework zkClient, PathChildrenCacheEvent event) throws Exception {
                if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)){
                    ChildData data = event.getData();
                    if(data!=null){
                        String lockPath = data.getPath();
                        if(lockPath!=null&&lockPath.equals(pathLock)){
                            System.out.println("锁被释放,准备竞争");
                            countDownLatch.countDown();
                        }
                    }
                }
            }
        });
    }

    public void getLock(){
        while (true){
            try {
                zkClient.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(pathLock);
                //获取锁成功
                System.out.println("获取锁成功");
                return;
            } catch (Exception e) {
                System.out.println("获取锁失败...");
                if(countDownLatch.getCount()<=0){
                    countDownLatch=new CountDownLatch(1);
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    public boolean releaseLock(){
        try {
            if(zkClient.checkExists().forPath(pathLock)!=null){
                zkClient.delete().forPath(pathLock);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        System.out.println("释放锁成功");
        return true;
    }
}
