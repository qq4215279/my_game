package com.mumu.game.business.system.luban;

/**
 * SystemSwitch
 * TODO
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:49
 */
public enum SystemSwitch {
    GM()
    ;


    public boolean isGM() {
        return true;
    }

    public boolean notGM() {
        return false;
    }

}
