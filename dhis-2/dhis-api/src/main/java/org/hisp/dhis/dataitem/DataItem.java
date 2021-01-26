/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.dataitem;

import static lombok.AccessLevel.NONE;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.translation.TranslationProperty.NAME;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hisp.dhis.common.cache.TranslationPropertyCache;
import org.hisp.dhis.translation.Translation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@JacksonXmlRootElement( localName = "dataItem", namespace = DXF_2_0 )
public class DataItem implements Serializable
{
    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String name;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Getter( value = NONE )
    @Setter( value = NONE )
    private String displayName;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String id;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String code;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String dimensionItemType;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String programId;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String combinedId;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String valueType;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String simplifiedValueType;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @Getter( value = NONE )
    @Setter( value = NONE )
    private Set<Translation> translations;

    @Getter( value = NONE )
    @Setter( value = NONE )
    private transient final TranslationPropertyCache translationPropertyCache = new TranslationPropertyCache();

    public String getDisplayName()
    {
        return translationPropertyCache.getOrDefault( NAME, getName() );
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "translations", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "translation", namespace = DXF_2_0 )
    public Set<Translation> getTranslations()
    {
        translations = translations != null ? translations : new HashSet<>();
        return translations;
    }

    /**
     * Clears out cache when setting translations.
     */
    public void setTranslations( final Set<Translation> translations )
    {
        translationPropertyCache.clear();
        this.translations = translations;
        translationPropertyCache.loadIfEmpty( translations );
    }
}
