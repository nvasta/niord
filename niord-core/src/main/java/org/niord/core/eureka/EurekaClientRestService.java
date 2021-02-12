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

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;

/**
 * REST interface to the Eureka Client
 */
@Path("/")
@Stateless
@SecurityDomain("keycloak")
@PermitAll
@SuppressWarnings("unused")
public class EurekaClientRestService {

    @Inject
    EurekaClientService eurekaClientService;

    /**
     * Returns the current service status.
     *
     * @return the current service status
     */
    @GET
    @Path("/status")
    @PermitAll
    @NoCache
    public String clientStatus() {
        return eurekaClientService.getStatus();
    }

    /**
     * Returns the health of the service.
     *
     * @return the health of the service
     */
    @GET
    @Path("/health")
    @PermitAll
    @NoCache
    public String health() {
        return "alive";
    }

    /**
     * Returns information About Niord.
     *
     * @return the information about Niord
     */
    @GET
    @Path("/about")
    @PermitAll
    @NoCache
    public String about() {
        return "Niord - Nautical Information Directory";
    }

}
