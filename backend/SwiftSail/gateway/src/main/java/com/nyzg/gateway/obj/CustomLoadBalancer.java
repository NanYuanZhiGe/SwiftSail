package com.nyzg.gateway.obj;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

public class CustomLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    private final ObjectProvider<ServiceInstanceListSupplier> objectProvider;
    private final String serviceId;
    private final AtomicInteger counter = new AtomicInteger(0);

    public CustomLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> objectProvider, String serviceId) {
        this.objectProvider = objectProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = objectProvider.getIfAvailable();
        assert supplier != null;
        return supplier.get().next().map(serviceInstances -> {
            int index = counter.incrementAndGet() % serviceInstances.size();
            return new DefaultResponse(serviceInstances.get(index));
        });
    }
}
