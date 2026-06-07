package com.mumu.game.template.func.dao;

import com.mumu.game.template.func.domain.PlayerTemplateState;
import org.springframework.stereotype.Component;

/**
 * PlayerTemplateStateManager
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:24
 */
@Component
public class PlayerTemplateStateManager {

    // TODO
    public PlayerTemplateState getOrNew(long playerId, int functionId) {
        return new PlayerTemplateState();
    }

    public void createOrUpdate(PlayerTemplateState sourcePlayerTemplateState) {
        PlayerTemplateState playerTemplateState = getOrNew(sourcePlayerTemplateState.getPlayerId(),
                sourcePlayerTemplateState.getFunctionId());
        playerTemplateState.setOpen(sourcePlayerTemplateState.isOpen());
        playerTemplateState.setHasRedPoint(playerTemplateState.isHasRedPoint());
        playerTemplateState.setActivityIds(playerTemplateState.getActivityIds());
        playerTemplateState.setStartTime(playerTemplateState.getStartTime());
        playerTemplateState.setEndTime(playerTemplateState.getEndTime());
        playerTemplateState.setClientParam(sourcePlayerTemplateState.getClientParam());
        playerTemplateState.setChangeFieldNameSet(sourcePlayerTemplateState.getChangeFieldNameSet());
    }
}
