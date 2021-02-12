/*
 * Copyright 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.core.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * The Eureka Client Implementation.
 */
@Singleton
@Startup
public class EurekaClientService {

    @Inject
    Logger log;

    // Service Variables
    private static ApplicationInfoManager applicationInfoManager;
    private static EurekaClient eurekaClient;

    /**
     * The Eureka Client Service Constructor.
     */
    public EurekaClientService() {

    }

    /**
     * Initialize the data store.
     */
    @PostConstruct
    protected void init() {
        this.applicationInfoManager = initializeApplicationInfoManager(new MyDataCenterInstanceConfig());
        this.applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);

        log.info("Initialising the Eureka Client...");

        this.eurekaClient = initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());
        this.applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);

        log.info("Eureka Client now UP...");
    }

    /**
     * Clean up the Netflix eureka client.
     */
    @PreDestroy
    protected void destroy() {
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
    }

    /**
     * Initialises the Netflix application info manager instance.
     *
     * @param instanceConfig the instance config
     * @return the initialised application info manager instance
     */
    protected static synchronized ApplicationInfoManager initializeApplicationInfoManager(EurekaInstanceConfig instanceConfig) {
        if (applicationInfoManager == null) {
            InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
            applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
        }

        return applicationInfoManager;
    }

    /**
     * Initialises the Netflix eureka client instance.
     *
     * @param applicationInfoManager the application info manager
     * @param clientConfig the eureka client configuration
     * @return the eureka client
     */
    protected static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig) {
        if (eurekaClient == null) {
            eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
        }

        return eurekaClient;
    }

    /**
     * Returns the current service status.
     *
     * @return the service status
     */
    public String getStatus() {
        return applicationInfoManager.getInfo().getStatus().toString();
    }

}
