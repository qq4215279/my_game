package com.mumu.game.gm.service;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ReflectUtil;
import com.mumu.game.business.item.BaseItem;
import com.mumu.game.business.item.luban.ItemConfigManager;
import com.mumu.game.core.cmd.response.ResponseResult;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.core.drop.core.ItemFlag;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.core.utils.JsonDocument;
import com.mumu.game.gm.anno.GmConfig;
import com.mumu.game.gm.enums.GmCommand;
import com.mumu.game.proto.message.core.ErrorCode;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GmServiceImpl
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/5 20:14
 */
@Service
public class GmServiceImpl implements GmService {
    /** GmCmd 与 被标注的方法映射 */
    private static final Map<GmCommand, Method> CMD_METHOD_MAP = new HashMap<>(4);

    static {
        Method[] methods = ReflectUtil.getMethods(GmServiceImpl.class);

        for (Method method : methods) {
            GmConfig gmConfig = method.getAnnotation(GmConfig.class);
            if (gmConfig != null){
                CMD_METHOD_MAP.put(gmConfig.gmCmd(), method);
            }
        }
    }



    @Override
    public ResponseResult executeGmCmd(long playerId, String key, List<String> args) {
        // gm开关校验
        /*if (!ConfigSwitchEnum.GM.getBool()) {
            return ResponseResult.errorByNotOpen(playerId);
        }*/

        // 命令校验
        GmCommand gmCommand = GmCommand.getGmCmdEnum(key);
        if (gmCommand == null){
            return ResponseResult.errorByParam(playerId);
        }

        // 转发到game服执行gm
        /*// TODO 其他服
        if (gmCommand.getExeServerGroup() == ServerGroup.GAME) {
            WGExecuteGmCmdMessage wgMsg = new WGExecuteGmCmdMessage();
            wgMsg.setKey(key);
            wgMsg.setArgs(args);
            MessageSender.sendToPlayerServer(
                    ServerGroup.GAME, MessageSender.proxy(Cmd.WGExecuteGmCmd, wgMsg, playerId));
            return ResponseResult.success(playerId, Cmd.WCExecuteGmCmd);
        }*/

        Method method = CMD_METHOD_MAP.get(gmCommand);
        if (method == null) {
            return ResponseResult.errorByParam(playerId);
        }

        try {
            method.setAccessible(true);
            // 构建结果
            Pair<ErrorCode, String> pair = (Pair<ErrorCode, String>) method.invoke(this, playerId, args);
            // WCExecuteGmCmdMessage resMsg = new WCExecuteGmCmdMessage();
            // resMsg.setRes(pair.getValue());
            // return ResponseResult.of(playerId, pair.getKey(), Cmd.WCExecuteGmCmd, resMsg);
            return ResponseResult.success(playerId);

        } catch (IllegalAccessException | InvocationTargetException e) {

            throw new RuntimeException(e);
        }
    }


    /**
     * 帮助gm
     * @param playerId 玩家id
     * @param args 参数列表
     * @since 2024/9/20 14:55
     */
    @GmConfig(gmCmd = GmCommand.HELP)
    private Pair<ErrorCode, String> help(long playerId, List<String> args) {
        JsonDocument doc = new JsonDocument();

        doc.startObject();
        doc.createElement("httpExecute", "http://{ip}:8388/gm/exeGmCommand?{example}");
        doc.createElement("devEg", "http://game-dev.baloot-xy.com:8388/gm/exeGmCommand?key=help");

        doc.startArray("gmCmd");
        for (GmCommand gmCommand : GmCommand.values()) {
            doc.startObject();
            doc.createElement("name", gmCommand.getKey());
            doc.createElement("desc", gmCommand.getDesc());
            doc.createElement("example", gmCommand.getExample());
            doc.createElement("count", gmCommand.getCount());
            doc.endObject();
        }
        doc.endArray();
        doc.endObject();

        return Pair.of(ErrorCode.SUCCESS, doc.toString());
    }

    /**
     * 增加道具
     * @param playerId 玩家id
     * @param args 参数列表
     * @return cn.hutool.core.lang.Pair<com.game.proto.core.ErrorCode,java.lang.String>
     * @since 2024/10/11 14:51
     */
    @GmConfig(gmCmd = GmCommand.ADD_ITEM)
    private Pair<ErrorCode, String> addItem(long playerId, List<String> args) {
        if (args.isEmpty()) {
            return Pair.of(ErrorCode.FAIL, "");
        }

        String dropStr = args.get(0);
        // Drop.of(dropStr).rewardItem(playerId, CurrencyAction.GM);

        return Pair.of(ErrorCode.SUCCESS, "");
    }

    /**
     * 一键所有道具
     * @param playerId playerId
     * @param args args
     * @return cn.hutool.core.lang.Pair<com.game.proto.core.ErrorCode,java.lang.String>
     * @since 2024/11/19 11:20
     */
    @GmConfig(gmCmd = GmCommand.ADD_ALL_ITEM)
    private Pair<ErrorCode, String> addAllItem(long playerId, List<String> args) {
        // 默认300000
        long num = 300000;
        if (!args.isEmpty()) {
            num = Long.parseLong(args.get(0));
        }

        StringBuilder sb = new StringBuilder();
        Collection<BaseItem> baseItemList = ItemConfigManager.getBaseItemList();
        /*for (BaseItem baseItem : baseItemList) {
            // 计时道具
            if (baseItem.getItemFlag() == ItemFlag.TIME) {
                sb.append(RewardEnum.buildReward(baseItem.getItemId(), num, 84600)).append(";");

                // 计数道具
            } else {
                RewardEnum.buildReward(baseItem.getItemId(), num);
                sb.append(RewardEnum.buildReward(baseItem.getItemId(), num)).append(";");
            }
        }

        Drop.of(sb.toString()).rewardItem(playerId, CurrencyAction.GM_ALL);
*/
        return Pair.of(ErrorCode.SUCCESS, "");
    }
}
