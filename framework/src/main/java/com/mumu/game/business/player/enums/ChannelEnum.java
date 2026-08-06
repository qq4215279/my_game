package com.mumu.game.business.player.enums;

import com.mumu.game.business.shop.luban.ShopConfigManager;
import com.mumu.game.charge.conf.ConfigPayID;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChannelEnum
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 17:43
 */
public enum ChannelEnum {
    /** dev */
    DEV {
        @Override
        public String getProductId(int goodsId) {
            return "";
        }
    },
    /** 华为 */
    HUAWEI {
        @Override
        public String getProductId(int goodsId) {
            ConfigPayID configPayID = ShopConfigManager.getConfigPayID(goodsId);
            if (configPayID == null) {
                return "";
            }
            return configPayID.getHuawei();
        }
    },
    /** 苹果ios */
    IOS {
        @Override
        public String getProductId(int goodsId) {
            ConfigPayID configPayID = ShopConfigManager.getConfigPayID(goodsId);
            if (configPayID == null) {
                return "";
            }
            return configPayID.getIos();
        }
    },
    /** 谷歌 */
    GOOGLEPLAY {
        @Override
        public String getProductId(int goodsId) {
            ConfigPayID configPayID = ShopConfigManager.getConfigPayID(goodsId);
            if (configPayID == null) {
                return "";
            }
            return configPayID.getGoogleplay();
        }
    },

    /** 艾克索拉（第三方支付） */
    PROD {
        @Override
        public String getProductId(int goodsId) {
            ConfigPayID configPayID = ShopConfigManager.getConfigPayID(goodsId);
            if (configPayID == null) {
                return "";
            }
            return configPayID.getProd();
        }
    },
    ;

    /**
     * 获取productId
     * @param goodsId 商品id
     * @return java.lang.String
     * @since 2025/1/17 12:07
     */
    public abstract String getProductId(int goodsId);

    public String getChannel() {
        return this.name().toLowerCase();
    }

    /** 渠道类型map */
    private static final Map<String, ChannelEnum> CHANNEL_MAP = Arrays.stream(values()).collect(
            Collectors.toMap(ChannelEnum::getChannel, nEnum -> nEnum));

    /**
     * 获取渠道枚举
     * @param channel channel
     * @return com.game.business.player.common.ChannelEnum
     * @since 2025/1/17 12:07
     */
    public static ChannelEnum getChannelEnum(String channel) {
        return CHANNEL_MAP.getOrDefault(channel, DEV);
    }

}
