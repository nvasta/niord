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

package org.niord.model.publication;

import io.swagger.annotations.ApiModel;
import org.niord.model.IJsonSerializable;
import org.niord.model.ILocalizable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;

/**
 * Value object for the {@code Publication} model entity
 */
@ApiModel(value = "Publication", description = "Publication model")
@XmlRootElement(name = "publication")
@XmlType(propOrder = {
        "publicationId", "category", "type", "descs"
})
public class PublicationVo implements ILocalizable<PublicationDescVo>, IJsonSerializable {

    String publicationId;
    PublicationCategoryVo category;
    PublicationType type;
    Date publishDateFrom;
    Date publishDateTo;
    List<PublicationDescVo> descs;


    /** {@inheritDoc} */
    @Override
    public PublicationDescVo createDesc(String lang) {
        PublicationDescVo desc = new PublicationDescVo();
        desc.setLang(lang);
        checkCreateDescs().add(desc);
        return desc;
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/


    public String getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(String publicationId) {
        this.publicationId = publicationId;
    }

    public PublicationCategoryVo getCategory() {
        return category;
    }

    public void setCategory(PublicationCategoryVo category) {
        this.category = category;
    }

    public PublicationType getType() {
        return type;
    }

    public void setType(PublicationType type) {
        this.type = type;
    }

    public Date getPublishDateFrom() {
        return publishDateFrom;
    }

    public void setPublishDateFrom(Date publishDateFrom) {
        this.publishDateFrom = publishDateFrom;
    }

    public Date getPublishDateTo() {
        return publishDateTo;
    }

    public void setPublishDateTo(Date publishDateTo) {
        this.publishDateTo = publishDateTo;
    }

    @Override
    public List<PublicationDescVo> getDescs() {
        return descs;
    }

    @Override
    public void setDescs(List<PublicationDescVo> descs) {
        this.descs = descs;
    }
}