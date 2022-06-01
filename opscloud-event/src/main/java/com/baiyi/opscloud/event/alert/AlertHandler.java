package com.baiyi.opscloud.event.alert;

import com.baiyi.opscloud.common.constants.enums.DsTypeEnum;
import com.baiyi.opscloud.common.datasource.ZabbixConfig;
import com.baiyi.opscloud.common.redis.RedisUtil;
import com.baiyi.opscloud.common.util.BeetlUtil;
import com.baiyi.opscloud.common.util.TimeUtil;
import com.baiyi.opscloud.core.factory.DsConfigHelper;
import com.baiyi.opscloud.core.provider.base.common.SimpleDsInstanceProvider;
import com.baiyi.opscloud.datasource.message.notice.DingtalkSendHelper;
import com.baiyi.opscloud.domain.constants.BusinessTypeEnum;
import com.baiyi.opscloud.domain.generator.opscloud.*;
import com.baiyi.opscloud.domain.param.datasource.DsInstanceParam;
import com.baiyi.opscloud.service.event.EventBusinessService;
import com.baiyi.opscloud.service.event.EventService;
import com.baiyi.opscloud.service.message.MessageTemplateService;
import com.baiyi.opscloud.service.server.ServerService;
import com.baiyi.opscloud.service.tag.SimpleTagService;
import com.baiyi.opscloud.zabbix.constant.SeverityType;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2022/5/31 13:42
 * @Version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertHandler extends SimpleDsInstanceProvider {

    private final EventService eventService;

    private final EventBusinessService eventBusinessService;

    private final SimpleTagService simpleTagService;

    private final ServerService serverService;

    private final MessageTemplateService messageTemplateService;

    protected static final int dsInstanceBusinessType = BusinessTypeEnum.DATASOURCE_INSTANCE.getType();

    protected static final String EVENT_TAG = "Event";

    private static final String ZBX_ALERT = "ZBX_ALERT";

    private final DingtalkSendHelper dingtalkSendHelper;

    private final DsConfigHelper dsConfigHelper;

    private final RedisUtil redisUtil;

    private static final String PREFIX = "zbx_alert";

    /**
     * 发送告警
     */
    public void sendTask() {
        List<DatasourceInstance> instances = listInstance();
        if (CollectionUtils.isEmpty(instances)) {
            log.info("告警任务结束: 无可用实例！");
            return;
        }
        for (DatasourceInstance instance : instances) {
            instanceSend(instance);
        }
    }

    private void instanceSend(DatasourceInstance instance) {
        Map<String, List<Event>> eventMap = Maps.newHashMap();
        Map<String, String> hostMap = Maps.newHashMap();
        List<Event> events = eventService.queryEventByInstance(instance.getUuid());
        if (CollectionUtils.isEmpty(events)) return;
        events.forEach(event -> {
            List<EventBusiness> eventBusinesses = eventBusinessService.queryByEventId(event.getId());
            if (CollectionUtils.isEmpty(eventBusinesses)) return;
            eventBusinesses.forEach(eventBusiness -> {
                final String key = eventBusiness.getName();
                if (eventMap.containsKey(key)) {
                    insertEventMap(eventMap, key, event);
                } else {
                    eventMap.put(key, Lists.newArrayList(event));
                }
                insertHostMap(hostMap, key, eventBusiness);
            });
        });
        for (String key : eventMap.keySet()) {
            String msg = renderTemplate(key, eventMap.get(key), hostMap);
            DatasourceConfig datasourceConfig = dsConfigHelper.getConfigByInstanceUuid(instance.getUuid());
            ZabbixConfig zabbixConfig = dsConfigHelper.build(datasourceConfig, ZabbixConfig.class);
            String cacheKey = Joiner.on("#").join(PREFIX, key);
            if (redisUtil.hasKey(cacheKey)) {
                log.info("告警静默: cacheKey = " + cacheKey);
                return; // 静默
            }
            dingtalkSendHelper.send(zabbixConfig.getZabbix(), msg);
            redisUtil.set(cacheKey, true, TimeUtil.minuteTime * 10 / 1000);
        }
    }

    private String renderTemplate(String host, List<Event> events, Map<String, String> hostMap) {
        Optional<Event> max = events.stream().max(Comparator.comparing(Event::getPriority));
        if (!max.isPresent()) return null;
        Map<String, Object> contentMap = Maps.newHashMap();
        contentMap.put("host", host);
        contentMap.put("ip", hostMap.getOrDefault(host, "-"));
        contentMap.put("severity", SeverityType.getName(max.get().getPriority()));
        contentMap.put("events", events);
        try {
            MessageTemplate messageTemplate = messageTemplateService.getByUniqueKey(ZBX_ALERT, "DINGTALK_APP", "markdown");
            if (messageTemplate == null) return null;
            return BeetlUtil.renderTemplate(messageTemplate.getMsgTemplate(), contentMap);
        } catch (IOException ioEx) {
            log.error(ioEx.getMessage());
            return null;
        }
    }

    private void insertHostMap(Map<String, String> hostMap, String key, EventBusiness eventBusiness) {
        if (hostMap.containsKey(key)) return;
        if (eventBusiness.getBusinessType() == BusinessTypeEnum.SERVER.getType()) {
            Server server = serverService.getById(eventBusiness.getBusinessId());
            if (server != null) {
                hostMap.put(key, server.getPrivateIp());
            }
        }
    }

    private void insertEventMap(Map<String, List<Event>> eventMap, String key, Event event) {
        List<Event> events = eventMap.get(key);
        events.add(event);
    }

    /**
     * 查询有效实例（包含标签）
     *
     * @return
     */
    protected List<DatasourceInstance> listInstance() {
        List<DatasourceInstance> instances = Lists.newArrayList();
        DsInstanceParam.DsInstanceQuery query = DsInstanceParam.DsInstanceQuery.builder().instanceType(getDsInstanceType().getName()).build();
        // 过滤掉没有标签的实例
        instances.addAll(dsInstanceService.queryByParam(query).stream().filter(e -> simpleTagService.hasBusinessTag(EVENT_TAG, dsInstanceBusinessType, e.getId())).collect(Collectors.toList()));
        return instances;
    }

    protected DsTypeEnum getDsInstanceType() {
        return DsTypeEnum.ZABBIX;
    }

}