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
