package com.mumu.game.core.net.session;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jctools.maps.NonBlockingHashSet;
import org.springframework.stereotype.Component;

import com.mumu.game.business.player.domain.Player;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.lock.InternalLock;
import com.mumu.game.core.lock.LockUtil;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.properties.CoreConfig;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.properties.ServerInfo;
import com.mumu.game.core.redis.RedisUtil;
import com.mumu.game.core.redis.constants.RedisKey;
import com.mumu.game.core.thread.ScheduledExecutorUtil;
import com.mumu.game.core.thread.ScheduledKey;
import com.mumu.game.core.utils.SpringContextUtils;

import jakarta.annotation.Resource;
import lombok.Getter;

/**
 * PlayerManager
 * 玩家管理器
 * @author liuzhen
 * @version 1.0.0 2026/5/4 09:59
 */
@Component
public class PlayerManager {
    public static PlayerManager self() {
        return SpringContextUtils.getBean(PlayerManager.class);
    }

    /** 玩家锁 - 分离锁 */
    private static final Object[] PLAYER_LOCKS = new Object[1024];

    /** 锁数量 */
    private static final int LOCK_LEN = PLAYER_LOCKS.length;

    static {
        for (int i = 0; i < PLAYER_LOCKS.length; i++) {
            PLAYER_LOCKS[i] = new Object();
        }
    }

    /**
     * 获取锁对象
     *
     * @param playerId 目标玩家id
     * @return java.lang.Object
     * @since 2025/8/4 14:51
     */
    private static Object getLockObj(long playerId) {
        int index = (int) (playerId % LOCK_LEN);
        return PLAYER_LOCKS[index];
    }

    @Resource
    CoreConfig coreConfig;

    @Resource
    ServerInfo serverInfo;

    /** 全部在本服的玩家（即玩家缓存属于本服，但不一定是最新。比如玩家进入游戏后，部分大厅缓存不是最新） */
    @Getter
    private final NonBlockingHashSet<Long> inServerPlayers = new NonBlockingHashSet<>();

    /** 全部在线玩家 */
    @Getter private final NonBlockingHashSet<Long> onlinePlayers = new NonBlockingHashSet<>();


    /** 获取玩家缓存对象 */
    public Player getPlayer(long playerId) {
        Player player = getPlayerOrNullable(playerId);
        if (player == null) {
            // 加锁
            try (InternalLock ignored = LockUtil.lock(getLockObj(playerId))) {
                player = getPlayerOrNullable(playerId);
                if (player == null) {
                    player = new Player();
                    player.setPlayerId(playerId);
                    player.setRobot(RedisUtil.sHasKey(RedisKey.PLAYER_ROBOT_SET.buildKey(), playerId));
                    // TODO 缓存支持
                    // CacheDataApi.insert(playerId, player);
                }
            }
        }
        return player;
    }

    /**
     * 获取玩家缓存，可能为null
     */
    public Player getPlayerOrNullable(long playerId) {
        // TODO
        // return CacheDataApi.selectOne(Player.class, playerId);
        return null;
    }

    /** 玩家进入当前服 true-命中缓存上线 */
    public boolean enterServer(long playerId) {
        return enterServer(getPlayer(playerId));
    }

    /** 玩家进入当前服 true-命中缓存上线 */
    public boolean enterServer(Player player) {
        // 移除延迟删除缓存任务
        ScheduledExecutorUtil.cancel(ScheduledKey.PLAYER_DELAY_OFFLINE.getKey(player.getPlayerId()), true);

        // 玩家已经在本服，则无需处理
        if (inServer(player.getPlayerId())) {
            return true;
        }

        // 记录本服玩家ID
        inServerPlayers.add(player.getPlayerId());

        // TODO 异步加载玩家DB缓存模型 机器人不提前加载缓存模型，因为机器人不会有太多的数据
        if (!player.isRobot()) {
            // AutoModelManager.asyncPreLoadEntity(player.getPlayerId());
        }
        return false;
    }

    /** 玩家延迟下线 */
    public void delayExitServer(long playerId) {
        // 玩家不在本服，不处理
        if (notInServer(playerId)) {
            return;
        }

        // 标记离线
        getPlayer(playerId).onLeft();
        // 延迟下线
        ScheduledExecutorUtil.schedule(
                ScheduledKey.PLAYER_DELAY_OFFLINE.getKey(playerId),
                () -> exitServer(playerId),
                coreConfig.getCacheDelayOffline(),
                TimeUnit.MINUTES);
    }

    /** 玩家离开当前服 */
    public void exitServer(long playerId) {
        // 玩家不在本服，不处理
        // if (notInServer(playerId)) return;
        // 只要本服有玩家缓存就处理
        Player player = getPlayerOrNullable(playerId);
        if (player == null) {
            return;
        }

        // 移除本服玩家ID
        inServerPlayers.remove(playerId);
        // 移除缓存模型
        // CacheDataApi.delete(playerId, player);
        // 清除玩家DB缓存模型
        // AutoModelManager.asyncDeletePlayerCache(playerId);
        // 移除延迟删除缓存任务
        ScheduledExecutorUtil.cancel(ScheduledKey.PLAYER_DELAY_OFFLINE.getKey(playerId), false);
    }

    /** 获取位置信息 from Redis */
    private int getServerIdByRedis(long playerId, ServiceType serviceType, int moduleId) {
        return RedisUtil.hGetInt(
                RedisKey.PLAYER_POSITION.buildKey(playerId), serviceType.name() + Symbol.UNDERLINE + moduleId);
    }


    /** 判断玩家是否在线 */
    public boolean isOnline(long playerId) {
        return onlinePlayers.contains(playerId);
    }

    /** 获取在线玩家数量 */
    public int getOnline() {
        return onlinePlayers.size();
    }

    /** 玩家是否离线 */
    public boolean isLeft(long playerId) {
        Player player = getPlayerOrNullable(playerId);
        return player == null || player.isLeft();
    }

    /** 判断玩家是否不在本服 */
    public boolean notInServer(long playerId) {
        return !inServer(playerId);
    }

    /** 判断玩家是否在本服 */
    public boolean inServer(long playerId) {
        return inServerPlayers.contains(playerId);
    }

    /** 获取玩家所在指定服 ID */
    public int getServerId(long playerId, ServiceType serviceType) {
        return getServerId(playerId, serviceType, 0);
    }

    /** 获取玩家所在指定服 ID */
    public int getServerId(long playerId, ServiceType serviceType, int gameId) {
        Player player = getPlayer(playerId);
        /* if (serviceType == ServiceType.GAME && gameId == 0) {
            gameId = player.getGameId();
            Assert.isTrue(
                    gameId != 0, "获取玩家所在游戏服ServerId，必须指定GameId！playerId: {}, group: {}", playerId, group);
        }
        // 获取本地缓存
        int serverId = player.getServerId(serviceType, gameId);
        if (serverId != 0) return serverId; */
        // 缓存没有找到，则从redis中获取
        int serverId = getServerIdByRedis(playerId, serviceType, gameId);
        // TODO 缓存到本地
        if (serverId != 0) {
            player.addServerGroup(serviceType, serverId);
        }

        return serverId;
    }

    /** 销毁缓存模型 */
    public void destroy() {
        LogTopic.ACTION.info("容器关闭 PlayerManager 玩家批量下线处理...", getInServerPlayers().size());
        forPlayers(this::exitServer);
    }

    /** 批量处理全部在线玩家 */
    public void forPlayers(Consumer<Long> consumer) {
        this.getInServerPlayers().forEach(consumer);
    }

    public boolean isRobot(long playerId) {
        return false;
    }

    /** TODO 容器关闭时批量踢出玩家 */
    /* public void contextClosedKickPlayers(ExitReason reason) {
        LogTopic.ACTION.info("contextClosedKickPlayers 批量踢出玩家", reason, getInServerPlayers().size());
        MessageSender.kickOutPlayers(reason, getInServerPlayers());
    } */

    /** TODO 卸载离线玩家缓存数据 */

}

