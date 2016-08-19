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
package org.niord.core.category;

import org.niord.core.model.VersionedEntity;
import org.niord.model.DataFilter;
import org.niord.model.ILocalizable;
import org.niord.model.message.CategoryVo;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a specific named category, part of an category-hierarchy
 */
@Entity
@Cacheable
@NamedQueries({
        @NamedQuery(name="Category.findByLegacyId",
                query = "select c FROM Category c where c.legacyId = :legacyId"),
        @NamedQuery(name  = "Category.findRootCategories",
                query = "select distinct c from Category c left join fetch c.children where c.parent is null"),
        @NamedQuery(name  = "Category.findCategoriesWithDescs",
                query = "select distinct c from Category c left join fetch c.descs"),
        @NamedQuery(name  = "Category.findCategoriesWithIds",
                query = "select distinct c from Category c left join fetch c.descs where c.id in (:ids)"),
        @NamedQuery(name  = "Category.findByMrn",
                query = "select c from Category c left join fetch c.descs where c.mrn = :mrn"),
})
@SuppressWarnings("unused")
public class Category extends VersionedEntity<Integer> implements ILocalizable<CategoryDesc> {

    String legacyId;

    String mrn;

    boolean active = true;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH })
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entity", orphanRemoval = true)
    List<CategoryDesc> descs = new ArrayList<>();

    @Column(length = 256)
    String lineage;


    /** Constructor */
    public Category() {
    }


    /** Constructor */
    public Category(CategoryVo category) {
        this(category, DataFilter.get());
    }


    /** Constructor */
    public Category(CategoryVo category, DataFilter filter) {
        updateCategory(category, filter);
    }


    /** Updates this category from the given category */
    public void updateCategory(CategoryVo category, DataFilter filter) {

        DataFilter compFilter = filter.forComponent(Category.class);

        this.id = category.getId();
        this.mrn = category.getMrn();
        this.active = category.isActive();
        if (compFilter.includeParent() && category.getParent() != null) {
            parent = new Category(category.getParent(), filter);
        }
        if (compFilter.includeChildren() && category.getChildren() != null) {
            category.getChildren().stream()
                    .map(a -> new Category(a, filter))
                    .forEach(this::addChild);
        }
        if (category.getDescs() != null) {
            category.getDescs()
                    .forEach(desc -> createDesc(desc.getLang()).setName(desc.getName()));
        }
    }


    /** Converts this entity to a value object */
    public CategoryVo toVo(DataFilter filter) {

        DataFilter compFilter = filter.forComponent(Category.class);

        CategoryVo category = new CategoryVo();
        category.setId(id);
        category.setMrn(mrn);
        category.setActive(active);

        if (compFilter.includeChildren()) {
            children.forEach(child -> category.checkCreateChildren().add(child.toVo(compFilter)));
        }

        if (compFilter.includeParent() && parent != null) {
            category.setParent(parent.toVo(compFilter));
        } else if (compFilter.includeParentId() && parent != null) {
            CategoryVo parent = new CategoryVo();
            parent.setId(parent.getId());
            category.setParent(parent);
        }

        if (!descs.isEmpty()) {
            category.setDescs(getDescs(filter).stream()
                .map(CategoryDesc::toVo)
                .collect(Collectors.toList()));
        }
        return category;
    }


    /**
     * Checks if the values of the category has changed.
     * Only checks relevant values, not e.g. database id, created date, etc.
     * Hence we do not use equals()
     *
     * @param template the template to compare with
     * @return if the category has changed
     */
    @Transient
    public boolean hasChanged(Category template) {
        return !Objects.equals(mrn, template.getMrn()) ||
                !Objects.equals(active, template.isActive()) ||
                descsChanged(template) ||
                parentChanged(template);
    }


    /** Checks if the geometry has changed */
    private boolean descsChanged(Category template) {
        return descs.size() != template.getDescs().size() ||
                descs.stream()
                    .anyMatch(d -> template.getDesc(d.getLang()) == null ||
                            !Objects.equals(d.getName(), template.getDesc(d.getLang()).getName()));
    }

    /** Checks if the parents have changed */
    private boolean parentChanged(Category template) {
        return (parent == null && template.getParent() != null) ||
                (parent != null && template.getParent() == null) ||
                (parent != null && template.getParent() != null &&
                        !Objects.equals(parent.getId(), template.getParent().getId()));
    }

    /** {@inheritDoc} */
    @Override
    public CategoryDesc createDesc(String lang) {
        CategoryDesc desc = new CategoryDesc();
        desc.setLang(lang);
        desc.setEntity(this);
        getDescs().add(desc);
        return desc;
    }


    /**
     * Adds a child category, and ensures that all references are properly updated
     *
     * @param category the category to add
     */
    public void addChild(Category category) {
        children.add(category);
        category.setParent(this);
    }


    /**
     * Update the lineage to have the format "/root-id/.../parent-id/id"
     * @return if the lineage was updated
     */
    public boolean updateLineage() {
        String oldLineage = lineage;
        lineage = getParent() == null
                ? "/" + id + "/"
                : getParent().getLineage() + id + "/";
        return !lineage.equals(oldLineage);
    }


    /**
     * If the category is active, ensure that parent categories are active.
     * If the category is inactive, ensure that child categories are inactive.
     */
    @SuppressWarnings("all")
    public void updateActiveFlag() {
        if (active) {
            // Ensure that parent categories are active
            if (getParent() != null && !getParent().isActive()) {
                getParent().setActive(true);
                getParent().updateActiveFlag();
            }
        } else {
            // Ensure that child categories are inactive
            getChildren().stream()
                    .filter(Category::isActive)
                    .forEach(child -> {
                        child.setActive(false);
                        child.updateActiveFlag();
                    });
        }
    }

    /**
     * Checks if this is a root category
     *
     * @return if this is a root category
     */
    @Transient
    public boolean isRootCategory() {
        return parent == null;
    }


    /*************************/
    /** Getters and Setters **/
    /*************************/

    public String getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(String legacyId) {
        this.legacyId = legacyId;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public List<CategoryDesc> getDescs() {
        return descs;
    }

    @Override
    public void setDescs(List<CategoryDesc> descs) {
        this.descs = descs;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }

    public List<Category> getChildren() {
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
    }

    public String getLineage() {
        return lineage;
    }

    public void setLineage(String lineage) {
        this.lineage = lineage;
    }
}

