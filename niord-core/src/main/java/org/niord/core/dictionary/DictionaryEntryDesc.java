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
package org.niord.core.dictionary;

import org.niord.core.dictionary.vo.DictionaryEntryDescVo;
import org.niord.core.model.DescEntity;
import org.niord.model.ILocalizedDesc;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

/**
 * Localized contents for the DictionaryEntry entity
 */
@Entity
@Cacheable
@SuppressWarnings("unused")
public class DictionaryEntryDesc extends DescEntity<DictionaryEntry> {

    @NotNull
    private String value;

    /** Constructor */
    public DictionaryEntryDesc() {
    }


    /** Constructor */
    public DictionaryEntryDesc(DictionaryEntryDescVo desc) {
        super(desc);
        this.value = desc.getValue();
    }


    /** Converts this entity to a value object */
    public DictionaryEntryDescVo toVo() {
        DictionaryEntryDescVo desc = new DictionaryEntryDescVo();
        desc.setLang(lang);
        desc.setValue(value);
        return desc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean descDefined() {
        return ILocalizedDesc.fieldsDefined(value);
    }


    /** {@inheritDoc} */
    @Override
    public void copyDesc(ILocalizedDesc desc) {
        this.value = ((DictionaryEntryDesc)desc).getValue();
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
