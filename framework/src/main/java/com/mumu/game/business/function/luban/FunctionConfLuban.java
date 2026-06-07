package com.mumu.game.business.function.luban;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.mumu.game.business.function.luban.dto.FunctionDTO;

import cn.hutool.core.lang.tree.Tree;
import lombok.Getter;

/**
 * FunctionConfLuban
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:37
 */
@Component
public class FunctionConfLuban {
    /** 功能id:config */
    @Getter
    private static volatile ImmutableMap<Integer, FunctionDTO> functionMap = ImmutableMap.of();
    /** 全功能的id树 */
    @Getter
    private static volatile Tree<Integer> functionIdTree;

    /** 功能开放，有等级条件的功能id列表 */
    private List<Integer> lvLimitConditionFunctionIdList = Collections.emptyList();
    /** 功能开放，有vip等级条件的功能id列表 */
    private List<Integer> vipLvLimitConditionFunctionIdList = Collections.emptyList();
    /** 功能开放，有绑定账号条件的功能id列表 */
    private List<Integer> bindAccountConditionFunctionIdList = Collections.emptyList();

    /** 功能id 与 活动组件id列表 映射 */
    private Map<Integer, List<Integer>> functinIdActivityIdsMap = Collections.emptyMap();

    public int order() {
        return 5;
    }

    public void autoLoad() {
        //TODO
        /*Collection<ConfigFunction> configFunctions = getLubanLoader().getConfigFunctionMap().values();
        List<FunctionDTO> configFunctionDTOList = configFunctions.stream().map(FunctionDTO::new).toList();
        functionMap = ImmutableUtil.list2ImmMap(configFunctionDTOList, FunctionDTO::getFunctionId);

        List<TreeNode<Integer>> nodeList = CollUtil.newArrayList();

        functionMap.forEach(
                (functionId, configFunctionDTO) -> {
                    int parentId = configFunctionDTO.getParentId();
                    // 初始化校验
                    Assert.isFalse(functionId != 0 && parentId == -1, "未配置父id");
          *//*if (functionId != 0 && parentId == -1) {
            // 初始化校验
            throw new RuntimeException("未配置父id");
          }*//*

                    TreeNode<Integer> treeNode = new TreeNode<>(functionId,
                            parentId, configFunctionDTO.getName(), functionId);
                    nodeList.add(treeNode);
                });

        List<Tree<Integer>> build = TreeUtil.build(nodeList, -1);
        Assert.notEmpty(build, "功能配置表异常~");
    *//*if (build.isEmpty()) {
      // 初始化校验
      throw new RuntimeException("功能配置表异常~");
    }*//*

        functionIdTree = build.get(0);

        lvLimitConditionFunctionIdList =
                configFunctions.stream().filter(e -> e.getLvLimit() > 0).map(e -> Integer.parseInt(e.getData_id())).collect(
                        Collectors.toList());
        vipLvLimitConditionFunctionIdList =
                configFunctions.stream().filter(e -> e.getVipLimit() > 0).map(e -> Integer.parseInt(e.getData_id())).collect(Collectors.toList());
        bindAccountConditionFunctionIdList =
                configFunctions.stream().filter(ConfigFunction::getBindAccount).map(e -> Integer.parseInt(e.getData_id())).collect(Collectors.toList());

        functinIdActivityIdsMap = configFunctions.stream().collect(Collectors.toMap(
                o -> Integer.parseInt(o.getData_id()),
                o -> Arrays.stream(o.getAcvitityType())
                        .map(Integer::valueOf)
                        .collect(Collectors.toList())
        ));*/

    }

    /**
     * getConfigFunction
     * @param functionId functionId
     * @return com.game.luban.system.function.ConfigFunction
     * @since 2025/2/11 11:43
     */
    public static FunctionDTO getConfigFunction(int functionId) {
        return functionMap.get(functionId);
    }

}
