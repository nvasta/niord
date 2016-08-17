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
package org.niord.model.message;

import io.swagger.annotations.ApiModel;
import org.apache.commons.lang.StringUtils;
import org.niord.model.IJsonSerializable;
import org.niord.model.ILocalizable;
import org.niord.model.geojson.FeatureCollectionVo;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Value object for the {@code Message} model entity
 */
@ApiModel(value = "Message", description = "Main NW and NM message class")
@XmlRootElement(name = "message")
@XmlType(propOrder = {
        "repoPath", "messageSeries", "number", "mrn", "shortId", "mainType", "type", "status",
        "areas", "categories", "charts", "horizontalDatum", "geometry", "publishDate", "dateIntervals",
        "references", "atonUids", "originalInformation", "descs", "attachments"
})
@SuppressWarnings("unused")
public class MessageVo implements ILocalizable<MessageDescVo>, IJsonSerializable {

    String id;
    String repoPath;
    Date created;
    Date updated;
    Integer version;
    MessageSeriesVo messageSeries;
    Integer number;
    String mrn;
    String shortId;
    MainType mainType;
    Type type;
    Status status;
    List<AreaVo> areas;
    List<CategoryVo> categories;
    List<ChartVo> charts;
    String horizontalDatum;
    FeatureCollectionVo geometry;
    Date publishDate;
    List<DateIntervalVo> dateIntervals;
    List<ReferenceVo> references;
    List<String> atonUids;
    Boolean originalInformation;
    List<MessageDescVo> descs;
    List<AttachmentVo> attachments;


    /** {@inheritDoc} */
    @Override
    public MessageDescVo createDesc(String lang) {
        MessageDescVo desc = new MessageDescVo();
        desc.setLang(lang);
        checkCreateDescs().add(desc);
        return desc;
    }

    /**
     * Sort all entity-owned descriptor records by the given language
     * @param language the language to sort by
     */
    public void sort(String language) {
        if (StringUtils.isNotBlank(language)) {
            sortDescs(language);
            if (getAttachments() != null) {
                getAttachments().forEach(att -> att.sortDescs(language));
            }
            if (getReferences() != null) {
                getReferences().forEach(ref -> ref.sortDescs(language));
            }
        }
    }


    /*************************/
    /** Collection Handling **/
    /*************************/

    public List<AreaVo> checkCreateAreas() {
        if (areas == null) {
            areas = new ArrayList<>();
        }
        return areas;
    }

    public List<CategoryVo> checkCreateCategories() {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        return categories;
    }

    public List<ChartVo> checkCreateCharts() {
        if (charts == null) {
            charts = new ArrayList<>();
        }
        return charts;
    }

    public List<DateIntervalVo> checkCreateDateIntervals() {
        if (dateIntervals == null) {
            dateIntervals = new ArrayList<>();
        }
        return dateIntervals;
    }

    public List<ReferenceVo> checkCreateReferences() {
        if (references == null) {
            references = new ArrayList<>();
        }
        return references;
    }

    public List<String> checkCreateAtonUids() {
        if (atonUids == null) {
            atonUids = new ArrayList<>();
        }
        return atonUids;
    }

    public List<AttachmentVo> checkCreateAttachments() {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        return attachments;
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    @XmlAttribute
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @XmlAttribute
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @XmlAttribute
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public MessageSeriesVo getMessageSeries() {
        return messageSeries;
    }

    public void setMessageSeries(MessageSeriesVo messageSeries) {
        this.messageSeries = messageSeries;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public MainType getMainType() {
        return mainType;
    }

    public void setMainType(MainType mainType) {
        this.mainType = mainType;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<AreaVo> getAreas() {
        return areas;
    }

    public void setAreas(List<AreaVo> areas) {
        this.areas = areas;
    }

    public List<CategoryVo> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryVo> categories) {
        this.categories = categories;
    }

    public List<ChartVo> getCharts() {
        return charts;
    }

    public void setCharts(List<ChartVo> charts) {
        this.charts = charts;
    }

    public String getHorizontalDatum() {
        return horizontalDatum;
    }

    public void setHorizontalDatum(String horizontalDatum) {
        this.horizontalDatum = horizontalDatum;
    }

    public FeatureCollectionVo getGeometry() {
        return geometry;
    }

    public void setGeometry(FeatureCollectionVo geometry) {
        this.geometry = geometry;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public List<DateIntervalVo> getDateIntervals() {
        return dateIntervals;
    }

    public void setDateIntervals(List<DateIntervalVo> dateIntervals) {
        this.dateIntervals = dateIntervals;
    }

    public List<ReferenceVo> getReferences() {
        return references;
    }

    public void setReferences(List<ReferenceVo> references) {
        this.references = references;
    }

    public List<String> getAtonUids() {
        return atonUids;
    }

    public void setAtonUids(List<String> atonUids) {
        this.atonUids = atonUids;
    }

    public Boolean getOriginalInformation() {
        return originalInformation;
    }

    public void setOriginalInformation(Boolean originalInformation) {
        this.originalInformation = originalInformation;
    }

    @Override
    public List<MessageDescVo> getDescs() {
        return descs;
    }

    @Override
    public void setDescs(List<MessageDescVo> descs) {
        this.descs = descs;
    }

    public List<AttachmentVo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentVo> attachments) {
        this.attachments = attachments;
    }
}