package com.mumu.common.mvc.cloud;

import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerServiceManager
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:43
 */
@Component
public class PlayerServiceManager implements ApplicationListener<GameChannelCloseEvent> {
    @Resource
    private BusinessServerManager businessServerManager;
    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 缓存PlayerID对应的所有的服务的实例的id，最外层的key是playerId，里面的Map的key是serviceId，value是serverId  (playerId:serviceId:serverId)
     */
    private Map<Long, Map<Integer, Integer>> serviceInstanceMap = new ConcurrentHashMap<>();
    /**
     * 创建一个事件线程，操作redis的时候，使用异步
     */
    private EventExecutor eventExecutor = new DefaultEventExecutor();

    @Override
    public void onApplicationEvent(GameChannelCloseEvent event) {
        this.serviceInstanceMap.remove(event.getPlayerId());
    }

    /**
     * @return java.util.Set<java.lang.Integer>
     * @date 2024/7/10 16:38
     */
    public Set<Integer> getAllServiceId() {
        return businessServerManager.getAllServiceId();
    }

    /**
     * @param playerId  playerId
     * @param serviceId serviceId
     * @param promise   promise
     * @return io.netty.util.concurrent.Promise<java.lang.Integer>
     * @date 2024/7/10 16:41
     */
    public Promise<Integer> selectServerId(long playerId, int serviceId, Promise<Integer> promise) {
        Map<Integer, Integer> instanceMap = this.serviceInstanceMap.getOrDefault(playerId, Collections.emptyMap());
        Integer serverId = null;

        // 如果在缓存中已存在，直接获取对应的serverId
        if (!instanceMap.isEmpty()) {
            serverId = instanceMap.get(serviceId);

            // 如果不存在，创建缓存对象
        } else {
            instanceMap = new ConcurrentHashMap<>();
            this.serviceInstanceMap.put(playerId, instanceMap);
        }

        if (serverId != null) {
            // 检测目前这个缓存的serverId的实例是否还有效，如果有效，直接返回
            if (businessServerManager.isEnableServer(serviceId, serverId)) {
                promise.setSuccess(serverId);
            } else {
                // 如果无效，设置为空，下面再重新获取
                serverId = null;
            }
        }

        // 重新获取一个新的服务实例serverId
        if (serverId == null) {
            eventExecutor.execute(() -> {
                try {
                    // 从redis查找一下，是否已由别的服务计算好
                    String key = this.getRedisKey(playerId);
                    Object value = redisTemplate.opsForHash().get(key, String.valueOf(serviceId));
                    boolean flag = true;
                    if (value != null) {
                        int serverIdOfRedis = Integer.parseInt((String) value);
                        flag = businessServerManager.isEnableServer(serviceId, serverIdOfRedis);
                        // 如果redis中已缓存且是有效的服务实例serverId，直接返回
                        if (flag) {
                            promise.setSuccess(serverIdOfRedis);
                            this.addLocalCache(playerId, serviceId, serverIdOfRedis);
                        }
                    }

                    // 如果Redis中没有缓存，或实例已失效，重新获取一个新的服务实例Id
                    if (value == null || !flag) {
                        Integer serverId2 = this.selectServerIdAndSaveRedis(playerId, serviceId);
                        this.addLocalCache(playerId, serviceId, serverId2);
                        promise.setSuccess(serverId2);
                    }
                } catch (Throwable e) {
                    promise.setFailure(e);
                }
            });
        }
        return promise;
    }

    private void addLocalCache(long playerId, int serviceId, int serverId) {
        Map<Integer, Integer> instanceMap = this.serviceInstanceMap.get(playerId);
        // 添加到本地缓存
        instanceMap.put(serviceId, serverId);
    }

    private String getRedisKey(Long playerId) {
        return "service_instance_" + playerId;
    }

    private Integer selectServerIdAndSaveRedis(Long playerId, Integer serviceId) {
        Integer serverId = businessServerManager.selectServerInfo(serviceId, playerId).getServerId();
        this.eventExecutor.execute(() -> {
            try {
                String key = this.getRedisKey(playerId);
                this.redisTemplate.opsForHash().put(key, String.valueOf(serviceId), String.valueOf(serverId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return serverId;
    }

}
