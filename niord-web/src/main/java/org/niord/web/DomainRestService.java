/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.niord.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.domain.Domain;
import org.niord.core.domain.DomainService;
import org.niord.model.vo.DomainVo;
import org.slf4j.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST interface for accessing domains.
 */
@Path("/domains")
@Stateless
@SecurityDomain("keycloak")
@PermitAll
public class DomainRestService {

    @Inject
    Logger log;

    @Inject
    DomainService domainService;


    /** Returns all domains */
    @GET
    @Path("/all")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<DomainVo> getAllDomains() {

        // Load the domains including their keycloak state
        return domainService.getDomains(true).stream()
                .map(Domain::toVo)
                .collect(Collectors.toList());
    }


    /** Returns all domains as a javascript */
    @GET
    @Path("/all.js")
    @Produces("application/javascript;charset=UTF-8")
    @GZIP
    public String getAllDomainsAsJavascript() {

        StringBuilder js = new StringBuilder()
                .append("niordDomains = ");
        try {
            ObjectMapper mapper = new ObjectMapper();
            js.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getAllDomains()));
        } catch (JsonProcessingException e) {
            js.append("[]");
        }

        js.append(";\n");
        return js.toString();
    }


    /** Creates a new domain */
    @POST
    @Path("/")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @RolesAllowed({ "sysadmin" })
    @GZIP
    @NoCache
    public DomainVo createDomain(DomainVo domain) throws Exception {
        log.info("Creating domain " + domain);
        return domainService.createDomain(new Domain(domain)).toVo();
    }

    /** Updates an existing domain */
    @PUT
    @Path("/{clientId}")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @RolesAllowed({ "sysadmin" })
    @GZIP
    @NoCache
    public DomainVo updateDomain(@PathParam("clientId") String clientId, DomainVo domain) throws Exception {
        if (!Objects.equals(clientId, domain.getClientId())) {
            throw new WebApplicationException(400);
        }

        log.info("Updating domain " + domain);
        return domainService.updateDomain(new Domain(domain)).toVo();
    }

    /** Deletes an existing domain */
    @DELETE
    @Path("/{clientId}")
    @Consumes("application/json;charset=UTF-8")
    @RolesAllowed({ "sysadmin" })
    @GZIP
    @NoCache
    public void deleteDomain(@PathParam("clientId") String clientId) throws Exception {
        log.info("Deleting domain " + clientId);
        domainService.deleteDomain(clientId);
    }


    /** Creates the domain in Keycloak */
    @POST
    @Path("/keycloak")
    @Consumes("application/json;charset=UTF-8")
    @RolesAllowed({ "sysadmin" })
    @GZIP
    @NoCache
    public void createDomainInKeycloak(DomainVo domain) throws Exception {
        log.info("Creating Keycloak domain " + domain);
        domainService.createDomainInKeycloak(new Domain(domain));
    }

}
