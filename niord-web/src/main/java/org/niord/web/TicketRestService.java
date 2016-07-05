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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.domain.DomainService;
import org.niord.core.user.TicketService;
import org.niord.core.user.UserService;
import org.slf4j.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import java.util.Set;

/**
 * The ticket service can be used to issue a one-time ticket with a short expiry time (1 minute).
 *
 * This is useful when e.g. creating a download link to a protected resource. Since
 * the download link is not an Ajax call, the usual Authorization header will not be
 * injected, and thus, the REST endpoint cannot be protected using the usual mechanism.
 * <p>
 * Instead, the download link should start by making an Ajax call to get a ticket, then
 * add the ticket to the link being opened as a ticket.
 * In the REST endpoint, the ticket can be validated programmatically.
 * <p>
 * The ticket is associated with various types of data, known to be valid when the ticket
 * was issued, i.e. the current domain, current user and optionally, a set of users roles.<br>
 * If in a subsequent request the ticket is resolved, then the stored ticket data can be trusted to be true
 * even in an un-authorized request.
 */
@Path("/tickets")
@Stateless
@SecurityDomain("keycloak")
public class TicketRestService {

    @Resource
    SessionContext ctx;

    @Inject
    Logger log;

    @Inject
    TicketService ticketService;

    @Inject
    UserService userService;

    @Inject
    DomainService domainService;

    /**
     * Returns a ticket to be used in a subsequent call.
     *
     * @param roles optionally, specify roles for which the ticket is issued
     * @return a new ticket
     */
    @GET
    @Path("/ticket")
    @Produces("text/plain")
    @NoCache
    @PermitAll
    public String getTicket(@QueryParam("role") Set<String> roles) {

        String[] ticketRoles = null;

        if (roles != null && !roles.isEmpty()) {
            // Ensure that the current user has all the roles that the ticket is requested for
            for (String role : roles) {
                if (!ctx.isCallerInRole(role)) {
                    throw new WebApplicationException(401);
                }
            }

            ticketRoles = roles.toArray(new String[roles.size()]);
        }

        return ticketService.createTicket(
                domainService.currentDomain(),
                userService.currentUser(),
                ticketRoles
        );
    }

}