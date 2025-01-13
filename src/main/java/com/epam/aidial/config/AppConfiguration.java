package com.epam.aidial.config;

import com.epam.aidial.kubernetes.knative.V1Service;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Yaml;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
public class AppConfiguration {
    @Getter
    private V1Secret secretConfig;
    private String secretConfigString;

    @Getter
    private V1Job jobConfig;
    private String jobConfigString;

    @Getter
    private V1Service serviceConfig;
    private String serviceConfigString;

    @Getter
    private V1Container templateContainer;
    private String templateContainerString;

    @Getter
    private V1Container builderContainer;
    private String builderContainerString;

    @Getter
    private V1Container serviceContainer;
    private String serviceContainerString;

    @Getter
    @Setter
    private Map<String, RuntimeConfiguration> runtimes;

    public void setSecretConfig(V1Secret secretConfig) {
        this.secretConfig = secretConfig;
        this.secretConfigString = Yaml.dump(secretConfig);
    }

    public void setJobConfig(V1Job jobConfig) {
        this.jobConfig = jobConfig;
        this.jobConfigString = Yaml.dump(jobConfig);
    }

    public void setServiceConfig(V1Service serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.serviceConfigString = Yaml.dump(serviceConfig);
    }

    public void setTemplateContainer(V1Container container) {
        this.templateContainer = container;
        this.templateContainerString = Yaml.dump(container);
    }

    public void setBuilderContainer(V1Container container) {
        this.builderContainer = container;
        this.builderContainerString = Yaml.dump(container);
    }

    public void setServiceContainer(V1Container container) {
        this.serviceContainer = container;
        this.serviceContainerString = Yaml.dump(container);
    }

    public V1Secret cloneSecretConfig() {
        return Yaml.loadAs(secretConfigString, V1Secret.class);
    }

    public V1Job cloneJobConfig() {
        return Yaml.loadAs(jobConfigString, V1Job.class);
    }

    public V1Service cloneServiceConfig() {
        return Yaml.loadAs(serviceConfigString, V1Service.class);
    }

    public V1Container cloneTemplateContainer() {
        return Yaml.loadAs(templateContainerString, V1Container.class);
    }

    public V1Container cloneBuilderContainer() {
        return Yaml.loadAs(builderContainerString, V1Container.class);
    }

    public V1Container cloneServiceContainer() {
        return Yaml.loadAs(serviceContainerString, V1Container.class);
    }

    @Data
    public static class RuntimeConfiguration {
        private String image;
        private String profile;
    }
}
