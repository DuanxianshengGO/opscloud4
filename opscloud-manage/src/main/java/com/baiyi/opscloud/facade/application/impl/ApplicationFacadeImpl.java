package com.baiyi.opscloud.facade.application.impl;

import com.baiyi.opscloud.common.base.AccessLevel;
import com.baiyi.opscloud.common.exception.auth.AuthenticationException;
import com.baiyi.opscloud.common.exception.common.OCException;
import com.baiyi.opscloud.common.util.BeanCopierUtil;
import com.baiyi.opscloud.common.util.IdUtil;
import com.baiyi.opscloud.common.util.SessionUtil;
import com.baiyi.opscloud.domain.DataTable;
import com.baiyi.opscloud.domain.ErrorEnum;
import com.baiyi.opscloud.domain.annotation.BusinessType;
import com.baiyi.opscloud.domain.annotation.TagClear;
import com.baiyi.opscloud.domain.constants.BusinessTypeEnum;
import com.baiyi.opscloud.domain.generator.opscloud.Application;
import com.baiyi.opscloud.domain.generator.opscloud.ApplicationResource;
import com.baiyi.opscloud.domain.generator.opscloud.UserPermission;
import com.baiyi.opscloud.domain.param.SimpleExtend;
import com.baiyi.opscloud.domain.param.application.ApplicationParam;
import com.baiyi.opscloud.domain.param.application.ApplicationResourceParam;
import com.baiyi.opscloud.domain.param.user.UserBusinessPermissionParam;
import com.baiyi.opscloud.domain.vo.application.ApplicationResourceVO;
import com.baiyi.opscloud.domain.vo.application.ApplicationVO;
import com.baiyi.opscloud.domain.vo.user.UserVO;
import com.baiyi.opscloud.facade.application.ApplicationFacade;
import com.baiyi.opscloud.facade.user.base.IUserBusinessPermissionPageQuery;
import com.baiyi.opscloud.facade.user.factory.UserBusinessPermissionFactory;
import com.baiyi.opscloud.factory.resource.AppResQueryFactory;
import com.baiyi.opscloud.factory.resource.IAppResQuery;
import com.baiyi.opscloud.packer.application.ApplicationPacker;
import com.baiyi.opscloud.packer.user.UserPermissionPacker;
import com.baiyi.opscloud.service.application.ApplicationResourceService;
import com.baiyi.opscloud.service.application.ApplicationService;
import com.baiyi.opscloud.service.auth.AuthRoleService;
import com.baiyi.opscloud.service.user.UserPermissionService;
import com.baiyi.opscloud.service.user.UserService;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2021/7/12 12:58 下午
 * @Version 1.0
 */
@Component
@RequiredArgsConstructor
@BusinessType(BusinessTypeEnum.APPLICATION)
public class ApplicationFacadeImpl implements ApplicationFacade, IUserBusinessPermissionPageQuery, InitializingBean {

    private final ApplicationService applicationService;

    private final ApplicationResourceService applicationResourceService;

    private final ApplicationPacker applicationPacker;

    private final UserService userService;

    private final AuthRoleService authRoleService;

    private final UserPermissionService userPermissionService;

    private final UserPermissionPacker userPermissionPacker;

    @Override
    public DataTable<ApplicationVO.Application> queryApplicationPage(ApplicationParam.ApplicationPageQuery pageQuery) {
        DataTable<Application> table = applicationService.queryPageByParam(pageQuery);
        List<ApplicationVO.Application> data = BeanCopierUtil.copyListProperties(table.getData(), ApplicationVO.Application.class).stream().peek(e -> applicationPacker.wrap(e, pageQuery)).collect(Collectors.toList());
        return new DataTable<>(data, table.getTotalNum());
    }

    @Override
    public DataTable<ApplicationVO.Application> queryMyApplicationPage(UserBusinessPermissionParam.UserBusinessPermissionPageQuery pageQuery) {
        pageQuery.setBusinessType(getBusinessType());
        if (isAdmin(SessionUtil.getUsername())) {
            pageQuery.setAdmin(true);
        } else {
            pageQuery.setUserId(userService.getByUsername(SessionUtil.getUsername()).getId());
        }
        DataTable<Application> table = applicationService.queryPageByParam(pageQuery);
        List<ApplicationVO.Application> data = BeanCopierUtil.copyListProperties(table.getData(), ApplicationVO.Application.class).stream().peek(e -> applicationPacker.wrap(e, pageQuery)).collect(Collectors.toList());
        return new DataTable<>(data, table.getTotalNum());
    }

    /**
     * 后期会删除，跨地域查询多应用性能太差
     *
     * @param pageQuery
     * @return
     */
    @Deprecated
    @Override
    public DataTable<ApplicationVO.Application> queryApplicationKubernetesPage(UserBusinessPermissionParam.UserBusinessPermissionPageQuery pageQuery) {
        pageQuery.setBusinessType(getBusinessType());
        if (isAdmin(SessionUtil.getUsername())) {
            pageQuery.setAdmin(true);
        } else {
            pageQuery.setUserId(userService.getByUsername(SessionUtil.getUsername()).getId());
        }
        DataTable<Application> table;
        if (IdUtil.isEmpty(pageQuery.getApplicationId())) {
            table = applicationService.queryPageByParam(pageQuery);
        } else {
            if (!pageQuery.getAdmin()) {
                UserPermission query = UserPermission.builder().businessType(BusinessTypeEnum.APPLICATION.getType()).businessId(pageQuery.getApplicationId()).userId(pageQuery.getUserId()).build();
                if (userPermissionService.getByUniqueKey(query) == null) {
                    throw new AuthenticationException(ErrorEnum.AUTHENTICATION_FAILURE);
                }
            }
            table = new DataTable<>(Lists.newArrayList(applicationService.getById(pageQuery.getApplicationId())), 1);
        }
        List<ApplicationVO.Application> data = BeanCopierUtil.copyListProperties(table.getData(), ApplicationVO.Application.class).stream().peek(e -> {
            if (IdUtil.isEmpty(pageQuery.getApplicationId())) {
                applicationPacker.wrap(e, pageQuery);
            } else {
                applicationPacker.wrap(e);
            }
        }).collect(Collectors.toList());
        return new DataTable<>(data, table.getTotalNum());
    }

    @Override
    public ApplicationVO.Application getApplicationKubernetes(ApplicationParam.GetApplicationKubernetes getApplicationKubernetes) {
        // 鉴权
        if (!isAdmin(SessionUtil.getUsername())) {
            int userId = userService.getByUsername(SessionUtil.getUsername()).getId();
            UserPermission query = UserPermission.builder().businessType(BusinessTypeEnum.APPLICATION.getType()).businessId(getApplicationKubernetes.getApplicationId()).userId(userId).build();
            if (userPermissionService.getByUniqueKey(query) == null) {
                throw new AuthenticationException(ErrorEnum.AUTHENTICATION_FAILURE);
            }
        }
        Application application = applicationService.getById(getApplicationKubernetes.getApplicationId());
        ApplicationVO.Application vo = BeanCopierUtil.copyProperties(application, ApplicationVO.Application.class);
        applicationPacker.wrap(vo, getApplicationKubernetes.getEnvType());
        return vo;
    }

    /**
     * OPS角色以上即认定为系统管理员
     *
     * @return
     */
    private boolean isAdmin(String username) {
        int accessLevel = authRoleService.getRoleAccessLevelByUsername(username);
        return accessLevel >= AccessLevel.OPS.getLevel();
    }

    @Override
    public DataTable<ApplicationResourceVO.Resource> previewApplicationResourcePage(ApplicationResourceParam.ResourcePageQuery pageQuery) {
        IAppResQuery appResQuery = AppResQueryFactory.getAppResQuery(pageQuery.getAppResType(), pageQuery.getBusinessType());
        if (appResQuery == null) {
            throw new OCException("无法预览应用资源，未找到对应的方法！");
        }
        return appResQuery.queryResourcePage(pageQuery);
    }

    @Override
    public ApplicationVO.Application getApplicationById(Integer id) {
        Application application = applicationService.getById(id);
        if (application == null) {
            throw new OCException(ErrorEnum.APPLICATION_NOT_EXIST);
        }
        ApplicationVO.Application vo = BeanCopierUtil.copyProperties(application, ApplicationVO.Application.class);
        applicationPacker.wrap(vo, SimpleExtend.EXTEND);
        return vo;
    }

    @Override
    public void addApplication(ApplicationVO.Application application) {
        if (applicationService.getByKey(application.getApplicationKey()) != null) {
            throw new OCException(ErrorEnum.APPLICATION_ALREADY_EXIST);
        }
        Application newApplication = BeanCopierUtil.copyProperties(application, Application.class);
        if (StringUtils.isNotBlank(application.getApplicationKey())) {
            newApplication.setApplicationKey(application.getApplicationKey().replaceAll(" ", "").toUpperCase());
        }
        applicationService.add(newApplication);
    }

    @Override
    public void updateApplication(ApplicationVO.Application application) {
        if (applicationService.getById(application.getId()) == null) {
            throw new OCException(ErrorEnum.APPLICATION_ALREADY_EXIST);
        }
        Application saveApplication = BeanCopierUtil.copyProperties(application, Application.class);
        if (StringUtils.isNotBlank(application.getApplicationKey())) {
            saveApplication.setApplicationKey(application.getApplicationKey().replaceAll(" ", "").toUpperCase());
        }
        applicationService.update(BeanCopierUtil.copyProperties(application, Application.class));
    }

    @Override
    @TagClear
    public void deleteApplication(Integer id) {
        if (!CollectionUtils.isEmpty(applicationResourceService.queryByApplication(id))) {
            throw new OCException(ErrorEnum.APPLICATION_RES_IS_NOT_EMPTY);
        }
        applicationService.deleteById(id);
    }

    @Override
    public void bindApplicationResource(ApplicationResourceVO.Resource resource) {
        if (applicationResourceService.getByUniqueKey(resource.getApplicationId(), resource.getBusinessType(), resource.getBusinessId()) != null) {
            throw new OCException(ErrorEnum.APPLICATION_RES_ALREADY_EXIST);
        }
        ApplicationResource res = BeanCopierUtil.copyProperties(resource, ApplicationResource.class);
        applicationResourceService.add(res);
    }

    @Override
    public void unbindApplicationResource(Integer id) {
        applicationResourceService.delete(id);
    }

    @Override
    public DataTable<UserVO.IUserPermission> queryUserBusinessPermissionPage(UserBusinessPermissionParam.UserBusinessPermissionPageQuery pageQuery) {
        pageQuery.setBusinessType(getBusinessType());
        DataTable<Application> table = applicationService.queryPageByParam(pageQuery);
        List<ApplicationVO.Application> data = BeanCopierUtil.copyListProperties(table.getData(), ApplicationVO.Application.class).stream().peek(e -> applicationPacker.wrap(e, pageQuery)).collect(Collectors.toList());
        if (pageQuery.getAuthorized()) {
            data.forEach(e -> {
                e.setUserId(pageQuery.getUserId());
                userPermissionPacker.wrap(e);
            });
        }
        return new DataTable<>(Lists.newArrayList(data), table.getTotalNum());
    }

    @Override
    public Integer getBusinessType() {
        return BusinessTypeEnum.APPLICATION.getType();
    }

    @Override
    public void afterPropertiesSet() {
        UserBusinessPermissionFactory.register(this);
    }

}
