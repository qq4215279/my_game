package com.mumu.game.business.function.dao;

import com.mumu.game.business.function.domain.PlayerTemplate;
import org.springframework.stereotype.Component;

/**
 * PlayerTemplateManager
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:35
 */
@Component
public class PlayerTemplateManager {
    public PlayerTemplate getOrNew(long playerId, int functionId) {
        return new PlayerTemplate();
    }

    /**
     * 查询PlayerTemplateDO（可能为空）
     * @param playerId playerId
     * @param functionId functionId
     * @return com.mumu.game.business.function.domain.PlayerTemplate
     */
    public PlayerTemplate getOrNull(long playerId, int functionId) {
        return new PlayerTemplate();
    }

    public void update(PlayerTemplate playerTemplateDO) {

    }
}
