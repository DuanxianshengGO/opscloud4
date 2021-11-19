package com.baiyi.opscloud.zabbix.provider;

import com.baiyi.opscloud.common.datasource.ZabbixConfig;
import com.baiyi.opscloud.core.factory.AssetProviderFactory;
import com.baiyi.opscloud.core.model.DsInstanceContext;
import com.baiyi.opscloud.domain.builder.asset.AssetContainer;
import com.baiyi.opscloud.domain.generator.opscloud.DatasourceInstance;
import com.baiyi.opscloud.domain.types.DsAssetTypeEnum;
import com.baiyi.opscloud.zabbix.convert.ZabbixHostAssetConvert;
import com.baiyi.opscloud.zabbix.provider.base.AbstractZabbixHostProvider;
import com.baiyi.opscloud.zabbix.v5.entity.ZabbixTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author <a href="mailto:xiuyuan@xinc818.group">修远</a>
 * @Date 2021/7/2 5:02 下午
 * @Since 1.0
 */

@Component
public class ZabbixHostTargetTriggerProvider extends AbstractZabbixHostProvider<ZabbixTrigger.Trigger> {

    @Resource
    private ZabbixHostTargetTriggerProvider zabbixHostTargetTriggerProvider;

    @Override
    protected List<com.baiyi.opscloud.zabbix.v5.entity.ZabbixHost.Host> listEntities(DsInstanceContext dsInstanceContext, ZabbixTrigger.Trigger target) {
        ZabbixConfig.Zabbix zabbix = buildConfig(dsInstanceContext.getDsConfig());
        return zabbixV5HostDatasource.listByTrigger(zabbix, target);

    }

    @Override
    public String getTargetAssetKey() {
        return DsAssetTypeEnum.ZABBIX_TRIGGER.getType();
    }


    @Override
    protected AssetContainer toAssetContainer(DatasourceInstance dsInstance, com.baiyi.opscloud.zabbix.v5.entity.ZabbixHost.Host entity) {
        return ZabbixHostAssetConvert.toAssetContainer(dsInstance, entity);
    }

    @Override
    public void afterPropertiesSet() {
        AssetProviderFactory.register(zabbixHostTargetTriggerProvider);
    }
}
