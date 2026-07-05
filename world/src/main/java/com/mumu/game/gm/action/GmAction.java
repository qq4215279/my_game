package com.mumu.game.gm.action;

import com.mumu.game.business.player.domain.Player;
import com.mumu.game.core.cmd.anno.CmdAction;
import com.mumu.game.core.cmd.anno.CmdMapping;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.gm.service.GmService;
import com.mumu.game.proto.message.core.ErrorCode;
import jakarta.annotation.Resource;

import java.util.List;

/**
 * GmAction
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/5 20:14
 */
@CmdAction
public class GmAction {

    @Resource
    private GmService gmService;

    /**
     * 执行gm
     * @param context context
     * @since 2024/9/20 14:10
     */
    // @CmdMapping(Cmd.CWExecuteGmCmd)
    public ResponseResult executeGmCmd(MessageContext context) {
        long playerId = context.getPlayerId();
        /*CWExecuteGmCmdMessage msg = context.getMsg(CWExecuteGmCmdMessage.class);
        if (ConfigSwitchEnum.notTest()) {
            LogTopic.ACTION.error("executeGmCmd", "context", context);
            return ResponseResult.errorByFail(playerId);
        }

        if (playerManager.notInServer(playerId)) {
            return ResponseResult.errorByParam(playerId);
        }
        // 玩家id校验
        Player player = playerManager.getPlayerOrNullable(playerId);
        if (player == null) {
            return ResponseResult.error(playerId, ErrorCode.FAIL_PLAYER_NOT_EXIST);
        }*/

        /** gm命令 */
        String key = "";
        /** gm命令参数 */
        List<String> args = new java.util.ArrayList<>();

        return gmService.executeGmCmd(playerId, key, args);
    }
}
