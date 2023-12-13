package com.baiyi.opscloud.controller.http;

import com.baiyi.opscloud.common.HttpResult;
import com.baiyi.opscloud.domain.param.kubernetes.KubernetesIngressParam;
import com.baiyi.opscloud.domain.param.kubernetes.KubernetesIstioParam;
import com.baiyi.opscloud.domain.param.kubernetes.KubernetesParam;
import com.baiyi.opscloud.domain.param.kubernetes.KubernetesServiceParam;
import com.baiyi.opscloud.domain.vo.application.ApplicationVO;
import com.baiyi.opscloud.facade.kubernetes.*;
import com.baiyi.opscloud.loop.kubernetes.KubernetesDeploymentResponse;
import io.fabric8.istio.api.networking.v1alpha3.DestinationRule;
import io.fabric8.istio.api.networking.v1alpha3.VirtualService;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * @Author baiyi
 * @Date 2023/5/22 16:54
 * @Version 1.0
 */
@RestController
@RequestMapping("/api/kubernetes")
@Tag(name = "Kubernetes")
@RequiredArgsConstructor
public class KubernetesController {

    private final KubernetesTerminalFacade kubernetesTerminalFacade;

    private final KubernetesIstioFacade istioFacade;

    private final KubernetesIngressFacade ingressFacade;

    private final KubernetesServiceFacade serviceFacade;

    private final KubernetesFacade kubernetesFacade;

    @Operation(summary = "按应用&环境查询无状态信息")
    @GetMapping(value = "/terminal/deployment/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<KubernetesDeploymentResponse<ApplicationVO.Kubernetes>> getKubernetesDeployment(@RequestParam @Valid int applicationId, @RequestParam @Valid int envType) {
        return new HttpResult<>(kubernetesTerminalFacade.getKubernetesDeployment(applicationId, envType));
    }

    @Operation(summary = "查询VirtualService")
    @PostMapping(value = "/istio/virtualService/get", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<VirtualService> getIstioVirtualService(@RequestBody KubernetesIstioParam.GetResource getResource) {
        return new HttpResult<>(istioFacade.getIstioVirtualService(getResource));
    }

    @Operation(summary = "更新VirtualService")
    @PutMapping(value = "/istio/virtualService/update", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<VirtualService> updateIstioVirtualService(@RequestBody KubernetesIstioParam.UpdateResource updateResource) {
        return new HttpResult<>(istioFacade.updateIstioVirtualService(updateResource));
    }

    @Operation(summary = "创建VirtualService")
    @PostMapping(value = "/istio/virtualService/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<VirtualService> createIstioVirtualService(@RequestBody KubernetesIstioParam.CreateResource createResource) {
        return new HttpResult<>(istioFacade.createIstioVirtualService(createResource));
    }

    @Operation(summary = "查询DestinationRule")
    @PostMapping(value = "/istio/destinationRule/get", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<DestinationRule> getIstioDestinationRule(@RequestBody KubernetesIstioParam.GetResource getResource) {
        return new HttpResult<>(istioFacade.getIstioDestinationRule(getResource));
    }

    @Operation(summary = "更新DestinationRule")
    @PutMapping(value = "/istio/destinationRule/update", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<DestinationRule> updateIstioDestinationRule(@RequestBody KubernetesIstioParam.UpdateResource updateResource) {
        return new HttpResult<>(istioFacade.updateIstioDestinationRule(updateResource));
    }

    @Operation(summary = "创建DestinationRule")
    @PostMapping(value = "/istio/destinationRule/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<DestinationRule> createIstioDestinationRule(@RequestBody KubernetesIstioParam.CreateResource createResource) {
        return new HttpResult<>(istioFacade.createIstioDestinationRule(createResource));
    }

    @Operation(summary = "查询Deployment")
    @PostMapping(value = "/deployment/get", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Deployment> getDeployment(@RequestBody KubernetesParam.GetResource getResource) {
        return new HttpResult<>(kubernetesFacade.getKubernetesDeployment(getResource));
    }

    // Ingress

    @Operation(summary = "查询Ingress")
    @PostMapping(value = "/ingress/get", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Ingress> getIngress(@RequestBody KubernetesIngressParam.GetResource getResource) {
        return new HttpResult<>(ingressFacade.getIngress(getResource));
    }

    @Operation(summary = "更新Ingress")
    @PutMapping(value = "/ingress/update", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Ingress> updateIngress(@RequestBody KubernetesIngressParam.UpdateResource updateResource) {
        return new HttpResult<>(ingressFacade.updateIngress(updateResource));
    }

    @Operation(summary = "创建Ingress")
    @PostMapping(value = "/ingress/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Ingress> createIngress(@RequestBody KubernetesIngressParam.CreateResource createResource) {
        return new HttpResult<>(ingressFacade.createIngress(createResource));
    }

    // Service

    @Operation(summary = "查询Service")
    @PostMapping(value = "/service/get", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Service> getService(@RequestBody KubernetesServiceParam.GetResource getResource) {
        return new HttpResult<>(serviceFacade.getService(getResource));
    }

    @Operation(summary = "更新Service")
    @PutMapping(value = "/service/update", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Service> updateService(@RequestBody KubernetesServiceParam.UpdateResource updateResource) {
        return new HttpResult<>(serviceFacade.updateService(updateResource));
    }

    @Operation(summary = "创建Service")
    @PostMapping(value = "/service/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<Service> createService(@RequestBody KubernetesServiceParam.CreateResource createResource) {
        return new HttpResult<>(serviceFacade.createService(createResource));
    }


}