/*
 * Copyright 2016 Danish Maritime Authority.
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
package org.niord.core.domain;

import org.apache.commons.lang.StringUtils;
import org.niord.core.area.Area;
import org.niord.core.category.Category;
import org.niord.core.keycloak.KeycloakIntegrationService;
import org.niord.core.message.MessageSeriesService;
import org.niord.core.service.BaseService;
import org.niord.core.user.UserService;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides an interface for managing application domains
 */
@SuppressWarnings("unused")
public class DomainService extends BaseService {

    @Inject
    Logger log;

    @Inject
    KeycloakIntegrationService keycloakService;

    @Inject
    MessageSeriesService messageSeriesService;

    @Inject
    UserService userService;


    /** Returns the current domain or null if none are set */
    public Domain currentDomain() {
        String clientId = userService.getCurrentResourceName();
        if (StringUtils.isNotBlank(clientId)) {
            return findByClientId(clientId);
        }
        return null;
    }


    /**
     * Returns the domain with the given clientId
     * @param clientId the clientId
     * @return the domain with the given clientId
     */
    public Domain findByClientId(String clientId) {
        try {
            return em
                    .createNamedQuery("Domain.findByClientId", Domain.class)
                    .setParameter("clientId", clientId)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Returns all domains
     * @return all domains
     */
    public List<Domain> getDomains() {
        return getDomains(false);
    }


    /**
     * Returns all domains, optionally with the current Keycloak state
     * @param keycloakState whether to load the state of the domains in Keycloak
     * @return all domains
     */
    public List<Domain> getDomains(boolean keycloakState) {
        List<Domain> domains = getAll(Domain.class);
        if (keycloakState) {
            try {
                Set<String> keycloakClients = keycloakService.getKeycloakDomainClientIds();
                domains.forEach(d -> d.setInKeycloak(keycloakClients.contains(d.getClientId())));
            } catch (Exception e) {
                log.error("Failed loading Keycloak states for domains" + e);
            }

        }
        return domains;
    }


    /**
     * Returns all domains for which the current user has the given role
     * @param role the role that the current user must have
     * @return all domains for which the current user has the given role
     */
    public List<Domain> domainsWithUserRole(String role) {
        Set<String> resourceNames = userService.getResourcesNamesWithRoles(role);
        return getDomains().stream()
                .filter(d -> resourceNames.contains(d.getClientId()))
                .collect(Collectors.toList());
    }


    /**
     * Updates the domain data from the domain template
     * @param domain the domain to update
     * @return the updated domain
     */
    public Domain updateDomain(Domain domain) {
        Domain original = findByClientId(domain.getClientId());
        if (original == null) {
            throw new IllegalArgumentException("Cannot update non-existing domain "
                    + domain.getClientId());
        }

        // Copy the domain data
        original.setName(domain.getName());
        original.setTimeZone(domain.getTimeZone());
        original.setLatitude(domain.getLatitude());
        original.setLongitude(domain.getLongitude());
        original.setZoomLevel(domain.getZoomLevel());

        // Substitute the areas with the persisted ones
        original.setAreas(persistedList(Area.class, domain.getAreas()));

        // Substitute the categories with the persisted ones
        original.setCategories(persistedList(Category.class, domain.getCategories()));

        // Substitute the message series with the persisted ones
        original.setMessageSeries(messageSeriesService.persistedMessageSeries(domain.getMessageSeries()));

        return saveEntity(original);
    }


    /**
     * Creates a new domain based on the domain template
     * @param domain the domain to create
     * @return the created domain
     */
    public Domain createDomain(Domain domain, boolean createInKeycloak) {
        Domain original = findByClientId(domain.getClientId());
        if (original != null) {
            throw new IllegalArgumentException("Cannot create domain with duplicate client ID "
                    + domain.getClientId());
        }

        // Substitute the areas with the persisted ones
        domain.setAreas(persistedList(Area.class, domain.getAreas()));

        // Substitute the categories with the persisted ones
        domain.setCategories(persistedList(Category.class, domain.getCategories()));

        // Substitute the message series with the persisted ones
        domain.setMessageSeries(messageSeriesService.persistedMessageSeries(domain.getMessageSeries()));

        domain = saveEntity(domain);

        // If request, create the domain in Keycloak - but do not throw an error in case of an error
        if (createInKeycloak) {
            try {
                keycloakService.createKeycloakDomainClient(domain);
            } catch (Exception e) {
                log.error("Error creating new domain in Keycloak", e);
            }
        }

        return domain;
    }


    /**
     * Deletes the domain
     * @param clientId the client id of the domain to delete
     */
    public boolean deleteDomain(String clientId) {

        Domain domain = findByClientId(clientId);
        if (domain != null) {
            remove(domain);
            return true;
        }
        return false;
    }


    /**
     * Creates the domain as a client in Keycloak
     * @param domain the domain to create a Keycloak client for
     */
    public void createDomainInKeycloak(Domain domain) throws Exception {

        long t0 = System.currentTimeMillis();

        boolean result = keycloakService.createKeycloakDomainClient(domain);

        log.info(String.format("Created client %s in Keycloak in %d ms. Result: %s",
                domain.getClientId(), System.currentTimeMillis() - t0, result));

    }
}
