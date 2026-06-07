package com.mumu.game.template.func.utils;

import cn.hutool.core.lang.tree.Tree;
import com.google.common.collect.Lists;
import com.mumu.game.business.function.luban.FunctionConfLuban;
import com.mumu.game.business.function.luban.dto.FunctionDTO;
import com.mumu.game.proto.function.ConfigFunctionInfoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TemplateUtil
 * 功能模版工具类
 * @author liuzhen
 * @version 1.0.0 2026/6/7 16:10
 */
public class TemplateUtil {
    /**
     * 构建功能配置基本信息
     * @param functionId functionId
     * @return com.game.proto.function.ConfigFunctionInfoBean
     * @since 2025/2/11 11:46
     */
    public static ConfigFunctionInfoBean buildConfigFunctionInfoBean(int functionId) {
        FunctionDTO configFunctionDTO = FunctionConfLuban.getConfigFunction(functionId);
        if (configFunctionDTO == null) {
            return null;
        }
        return configFunctionDTO.buildConfigFunctionInfoBean();
    }


    // ========================================================================>

    /**
     * 获取功能id的顶层下所有子功能id
     *
     * @param myId 功能id
     * @return java.util.List<java.lang.Integer>
     * @since 2024/11/19 19:30
     */
    public static List<Integer> getTopModuleAllFunctionId(int myId) {
        int parentId = getTopModuleParentFunctionId(myId);

        List<Integer> res = new ArrayList<>();
        getAllSubFunctionId(parentId, res);

        return res;
    }

    /**
     * 获取功能id所在模块的父功能id
     *
     * @return int
     * @since 2024/12/4 18:07
     */
    public static int getModuleParentFuncId(int myId) {
        FunctionDTO configFunctionDTO = FunctionConfLuban.getConfigFunction(myId);
        /*int moduleParentId = configFunctionDTO.getModuleParentId();
        if (moduleParentId != -1) {
            return moduleParentId;
        }*/

        // 找不到，默认找顶层父功能id
        return getTopModuleParentFunctionId(myId);
    }

    /**
     * 获取功能id的顶层父功能id（注：最顶层功能id(0)下层）
     *
     * @param myId 功能id
     * @return int
     * @since 2024/11/19 19:30
     */
    public static int getTopModuleParentFunctionId(int myId) {
        int parentId = myId;
        // 循环30次！避免死循环
        for (int i = 0; i < 30; i++) {
            Tree<Integer> node = FunctionConfLuban.getFunctionIdTree().getNode(parentId);
            // 顶级功能id
            if (node == null || node.getParentId() == null || node.getParentId() == 0) {
                break;
            }

            parentId = node.getParentId();
        }

        return parentId;
    }

    /**
     * 获取指定功能id下的所有子功能id（包括孙）
     * @param myId 功能id
     * @param res res
     * @since 2024/11/19 19:30
     */
    public static void getAllSubFunctionId(int myId, List<Integer> res) {
        res.add(myId);

        List<Integer> subFunctionIdList = getSubFunctionIdList(myId);
        if (subFunctionIdList.isEmpty()) {
            return;
        }

        for (int subId : subFunctionIdList) {
            getAllSubFunctionId(subId, res);
        }
    }

    /**
     * 获取子功能ids
     * @param myId 功能id
     * @return java.util.List<java.lang.Integer>
     * @since 2024/11/19 19:31
     */
    public static List<Integer> getSubFunctionIdList(int myId) {
        Tree<Integer> node = FunctionConfLuban.getFunctionIdTree().getNode(myId);
        if (node != null) {
            List<Tree<Integer>> children = node.getChildren();
            if (children != null) {
                return children.stream()
                        .filter(Objects::nonNull)
                        .map(Tree::getId)
                        .collect(Collectors.toList());
            }
        }
        return Lists.newArrayList();
    }
}
