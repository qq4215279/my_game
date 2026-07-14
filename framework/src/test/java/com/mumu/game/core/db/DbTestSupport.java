package com.mumu.game.core.db;

import java.lang.reflect.Field;

import com.mumu.game.core.db.anno.ModelTable;
import com.mumu.game.core.db.core.meta.ModelMeta;
import com.mumu.game.core.db.example.Player;
import com.mumu.game.core.db.example.PlayerTemplate;

/**
 * 数据层单测公共工具
 */
public final class DbTestSupport {

    private DbTestSupport() {
    }

    public static ModelMeta playerMeta() {
        return ModelMeta.parse(Player.class, Player.class.getAnnotation(ModelTable.class), "mongo");
    }

    /** 仅 DB 策略元数据（无 JVM / Redis） */
    public static ModelMeta dbOnlyPlayerMeta() {
        return ModelMeta.parse(DbOnlyPlayerEntity.class,
            DbOnlyPlayerEntity.class.getAnnotation(ModelTable.class), "mongo");
    }

    public static ModelMeta playerTemplateMeta() {
        return ModelMeta.parse(PlayerTemplate.class, PlayerTemplate.class.getAnnotation(ModelTable.class), "mongo");
    }

    public static Player newPlayer(long playerId, String name, int level) {
        Player player = new Player();
        player.setPlayerId(playerId);
        player.setName(name);
        player.setLevel(level);
        return player;
    }

    public static PlayerTemplate newPlayerTemplate(long playerId, int functionId, int activityId) {
        PlayerTemplate template = new PlayerTemplate();
        template.setPlayerId(playerId);
        template.setFunctionId(functionId);
        template.setActivityId(activityId);
        return template;
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> type = target.getClass();
            Field field = null;
            while (type != null) {
                try {
                    field = type.getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException e) {
                    type = type.getSuperclass();
                }
            }
            if (field == null) {
                throw new NoSuchFieldException(fieldName);
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("注入字段失败: " + fieldName, e);
        }
    }
}
