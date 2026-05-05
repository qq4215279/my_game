package com.mumu.game.core.autoluban.autoluban2.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.cxx.luban.lubanconfigclient.AbLubanConfigLoaderClient;
import com.cxx.luban.lubanconfigclient.ILubanConfigClient;
import com.game.framework.core.autoinit.AutoInitEvent;
import com.game.framework.core.autoinit.consts.AutoInitModule;
import com.game.framework.core.autoinit.manager.AutoInitManager;
import com.game.framework.core.autoluban.AutoLubanEvent;
import com.game.framework.core.autoluban.AutoLubanParam;
import com.game.framework.core.log.LogTopic;
import com.game.framework.core.utils.SpringContextUtils;
import com.game.framework.net.consts.ServerGroup;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/** 自动鲁班管理器 @Date: 2024/8/5 上午10:36 @Author: xu.hai */
@Component
@ConditionalOnBean(ILubanConfigClient.class)
public class AutoLubanManager implements AutoInitEvent {
  private static final LogTopic log = LogTopic.ACTION;

  private final Map<
          Class<? extends AbLubanConfigLoaderClient>,
          Set<AutoLubanEvent<? extends AbLubanConfigLoaderClient>>>
      autoLubanMap = Maps.newHashMap();

  /** AutoLubanEvent 比较器 */
  private static final Comparator<AutoLubanEvent<? extends AbLubanConfigLoaderClient>> COMPARATOR =
      Comparator.comparingInt((AutoLubanEvent<? extends AbLubanConfigLoaderClient> o) -> o.order())
          .thenComparingInt(Object::hashCode);

  @Override
  public void autoInit() {
    // 加载鲁班事件
    loadEvents();
    // 加载鲁班KV配置
    loadEnums(AutoInitManager.CLASSES);
    // 注册鲁班配置更新监听
    register();
  }

  @Override
  public AutoInitModule getInitGroup() {
    return AutoInitModule.LUBAN_LISTENER;
  }

  @Override
  public Collection<ServerGroup> loadServer() {
    return List.of(ServerGroup.values());
  }

  @Override
  public int order() {
    return 2;
  }

  /** 加载容器中 {@link AutoLubanEvent} 的实现 */
  @SuppressWarnings("unchecked")
  private void loadEvents() {
    Map<String, AutoLubanEvent> eventMap = SpringContextUtils.getBeansOfType(AutoLubanEvent.class);
    ServerGroup curr = ServerGroup.curr();
    for (AutoLubanEvent event : eventMap.values()) {
      if (event.loadServer().contains(curr))
        autoLubanMap
            .computeIfAbsent(event.getLubanLoaderType(), group -> Sets.newTreeSet(COMPARATOR))
            .add(event);
    }
    log.info(
        "AutoLubanManager.loadEvents",
        StrUtil.format(
            "扫描到[AutoLubanEvent]实现类[{}]个, 所属模块[{}]个!", eventMap.size(), autoLubanMap.size()));
  }

  /** 加载鲁班KV缓存 {@link AutoLubanParam} 的实现 */
  @SuppressWarnings("unchecked")
  private void loadEnums(Set<Class<?>> scan) {
    Set<AutoLubanEvent<? extends AbLubanConfigLoaderClient>> enumEvents =
        Sets.newTreeSet(COMPARATOR);
    ServerGroup curr = ServerGroup.curr();
    int count = 0;
    for (Class<?> clazz : scan) {
      if (AutoLubanParam.cannotLoad(clazz)) continue;

      Field[] fields = clazz.getFields();
      if (ArrayUtil.isEmpty(fields)) return;
      Class<? extends Enum> enumClazz = (Class<? extends Enum<?>>) clazz;
      for (Field field : fields) {
        Enum<?> anEnum = EnumUtil.fromStringQuietly(enumClazz, field.getName());
        if (anEnum == null) continue;
        AutoLubanEvent<? extends AbLubanConfigLoaderClient> event =
            (AutoLubanEvent<? extends AbLubanConfigLoaderClient>) anEnum;
        if (!event.loadServer().contains(curr)) continue;
        this.autoLubanMap
            .computeIfAbsent(event.getLubanLoaderType(), group -> Sets.newTreeSet(COMPARATOR))
            .add(event);
        enumEvents.add(event);
      }
      count++;
    }
    log.info(
        "AutoLubanManager.loadEvents",
        StrUtil.format("扫描到[AutoLubanParam]实现类[{}]个, Enum字段[{}]个!", count, enumEvents.size()));
    // 直接初始化
    execute(enumEvents);
  }

  /** 注册鲁班配置更新监听 */
  private void register() {
    autoLubanMap.forEach(
        (type, events) ->
            AbLubanConfigLoaderClient.putLoadEventConsumer(
                type, (loader, client) -> this.execute(events)));
  }

  /** 监听事件执行 */
  private void execute(Set<AutoLubanEvent<? extends AbLubanConfigLoaderClient>> events) {
    if (CollUtil.isEmpty(events)) return;
    for (AutoLubanEvent<? extends AbLubanConfigLoaderClient> event : events) {
      try {
        event.autoLubanRefresh();
      } catch (Exception e) {
        log.error(
            e,
            "AutoLubanManager execute error!",
            "loader",
            event.getLubanLoaderType(),
            "clazz",
            event.getClass());
      }
    }
    log.info("AutoLubanManager execute success!", events.size());
  }
}
