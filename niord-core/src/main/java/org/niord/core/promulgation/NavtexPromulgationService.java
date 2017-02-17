/*
 * Copyright 2017 Danish Maritime Authority.
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

package org.niord.core.promulgation;

import org.apache.commons.lang.StringUtils;
import org.niord.core.area.Area;
import org.niord.core.db.CriteriaHelper;
import org.niord.core.message.Message;
import org.niord.core.message.vo.SystemMessageVo;
import org.niord.core.promulgation.vo.BaseMessagePromulgationVo;
import org.niord.core.promulgation.vo.NavtexMessagePromulgationVo;
import org.niord.core.util.TextUtils;
import org.niord.model.DataFilter;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages NAVTEX-via-mailing-lists promulgations
 */
@Singleton
@Startup
@Lock(LockType.READ)
@SuppressWarnings("unused")
public class NavtexPromulgationService extends BasePromulgationService {

    /***************************************/
    /** Promulgation Service Handling     **/
    /***************************************/


    /** {@inheritDoc} */
    @Override
    public String getServiceId() {
        return NavtexMessagePromulgation.TYPE;
    }


    /** {@inheritDoc} */
    @Override
    public String getServiceName() {
        return "NAVTEX mailing list";
    }

    /***************************************/
    /** Message Life-cycle Management     **/
    /***************************************/


    /** {@inheritDoc} */
    @Override
    public void onLoadSystemMessage(SystemMessageVo message, PromulgationType type) throws PromulgationException {
        NavtexMessagePromulgationVo navtex = message.promulgation(NavtexMessagePromulgationVo.class, type.getTypeId());
        if (navtex == null) {
            navtex = new NavtexMessagePromulgationVo(type.toVo(DataFilter.get()));
            message.checkCreatePromulgations().add(navtex);
        }

        // Add all active transmitters not already added
        for (NavtexTransmitter transmitter : findTransmittersByAreas(null, true)) {
            if (!navtex.getTransmitters().containsKey(transmitter.getName())) {
                navtex.getTransmitters().put(transmitter.getName(), Boolean.FALSE);
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void onCreateMessage(Message message, PromulgationType type) throws PromulgationException {
        checkNavtexPromulgation(message, type);
    }


    /** {@inheritDoc} */
    @Override
    public void onUpdateMessage(Message message, PromulgationType type) throws PromulgationException {
        checkNavtexPromulgation(message, type);
    }


    /** {@inheritDoc} */
    @Override
    public void onUpdateMessageStatus(Message message, PromulgationType type) throws PromulgationException {
        checkNavtexPromulgation(message, type);
    }


    /** {@inheritDoc} */
    @Override
    public BaseMessagePromulgationVo generateMessagePromulgation(SystemMessageVo message, PromulgationType type) throws PromulgationException {

        NavtexMessagePromulgationVo navtex = new NavtexMessagePromulgationVo();

        // Add all active transmitters - by default, not selected
        findTransmittersByAreas(null, true)
                .forEach(t -> navtex.getTransmitters().put(t.getName(), Boolean.FALSE));

        // Select transmitters associated with the current message areas
        if (!message.getAreas().isEmpty()) {
            List<Area> areas = message.getAreas().stream()
                    .map(Area::new)
                    .collect(Collectors.toList());
            findTransmittersByAreas(areas, true)
                    .forEach(t -> navtex.getTransmitters().put(t.getName(), Boolean.TRUE));
        }

        String language = getLanguage(type);
        StringBuilder text = new StringBuilder();
        message.getParts().stream()
            .flatMap(p -> p.getDescs().stream())
            .filter(d -> d.getLang().equals(language))
            .filter(d -> StringUtils.isNotBlank(d.getDetails()))
            .map(d -> TextUtils.html2txt(d.getDetails()))
            .forEach(d -> text.append(d.toUpperCase()).append(System.lineSeparator()));

        if (text.length() > 0) {
            navtex.setPromulgate(true);
            navtex.setText(text.toString());
        } else {
            navtex.setPromulgate(false);
        }

        return navtex;
    }


    /**
     * Checks that the NAVTEX promulgation is valid and ready to be persisted
     * @param message the message to check
     */
    private void checkNavtexPromulgation(Message message, PromulgationType type) {
        NavtexMessagePromulgation navtex = message.promulgation(NavtexMessagePromulgation.class, type.getTypeId());
        if (navtex != null) {
            // Replace the list of transmitters with the persisted entities
            navtex.setTransmitters(persistedTransmitters(navtex.getTransmitters()));
        }
    }


    /***************************************/
    /** Transmitter Handling              **/
    /***************************************/


    /** Returns the transmitter with the given name, or null if not found **/
    public NavtexTransmitter findTransmitterByName(String name) {
        try {
            return em.createNamedQuery("NavtexTransmitter.findByName", NavtexTransmitter.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }


    /** Update the list of transmitters with the persisted entities **/
    protected List<NavtexTransmitter> persistedTransmitters(List<NavtexTransmitter> transmitters) {
        return transmitters.stream()
                .map(t -> findTransmitterByName(t.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    /**
     * Find all NAVTEX transmitters matching the given areas.
     * @param areas the areas to match
     * @return all NAVTEX transmitters matching the given areas.
     */
    @SuppressWarnings("all")
    public List<NavtexTransmitter> findTransmittersByAreas(List<Area> areas, boolean onlyActive) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<NavtexTransmitter> query = cb.createQuery(NavtexTransmitter.class);
        Root<NavtexTransmitter> transmitterRoot = query.from(NavtexTransmitter.class);

        CriteriaHelper<NavtexTransmitter> criteriaHelper = new CriteriaHelper<>(cb, query);

        if (onlyActive) {
            criteriaHelper.equals(transmitterRoot.get("active"), Boolean.TRUE);
        }


        if (areas != null && !areas.isEmpty()) {

            // Make sure we have managed entities, not template antities
            areas = persistedList(Area.class, areas);

            Join<NavtexTransmitter, Area> areaJoin = transmitterRoot.join("areas", JoinType.LEFT);
            Predicate[] areaMatch = areas.stream()
                    .map(a -> cb.like(areaJoin.get("lineage"), a.getLineage() + "%"))
                    .toArray(Predicate[]::new);
            criteriaHelper.add(cb.or(areaMatch));
        }

        query.select(transmitterRoot)
                .where(criteriaHelper.where())
                .orderBy(cb.asc(cb.lower(transmitterRoot.get("name"))));

        return em.createQuery(query).getResultList();
    }


    /** Returns all transmitters */
    public List<NavtexTransmitter> getTransmitters() {
        return getAll(NavtexTransmitter.class).stream()
                .collect(Collectors.toList());
    }

    /** Creates a new transmitter */
    public NavtexTransmitter createTransmitter(NavtexTransmitter transmitter) {
        log.info("Creating transmitter " + transmitter.getName());
        transmitter.setAreas(persistedList(Area.class, transmitter.getAreas()));
        return saveEntity(transmitter);
    }


    /** Updates an existing transmitter */
    public NavtexTransmitter updateTransmitter(NavtexTransmitter transmitter) {
        log.info("Updating transmitter " + transmitter.getName());
        NavtexTransmitter original = findTransmitterByName(transmitter.getName());
        original.setActive(transmitter.isActive());
        original.setAreas(persistedList(Area.class, transmitter.getAreas()));
        return saveEntity(original);
    }


    /** Deletes an existing transmitter */
    public boolean deleteTransmitter(String name) {
        log.info("Deleting transmitter " + name);
        NavtexTransmitter original = findTransmitterByName(name);
        if (original != null) {
            remove(original);
            return true;
        }
        return false;
    }

}
