package com.mumu.game.business.system.luban;

/**
 * SystemSwitch
 * TODO
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:49
 */
public enum SystemSwitch {
    /** gm 命令是否开启 */
    GM()
    ;


    public static boolean isGM() {
        return true;
    }

    public static boolean notGM() {
        return false;
    }

}
