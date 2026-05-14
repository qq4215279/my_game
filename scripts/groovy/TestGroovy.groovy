package com.game.framework.core.groovy

import com.game.groovy.IGroovyExecutor

/**
 * @since 2024/12/18 下午8:17
 * @Author: xu.hai
 */
class TestGroovy implements IGroovyExecutor {

    @Override
    Object execute(String... params) throws Exception {
//        def bean = SpringContextUtils.getBean(GameInfoService.class)
//        return AutoScheduleManager.getInfos()
        return 1111
    }
}
