/*
 * Copyright 2021 GLA UK Research and Development Directive
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
package org.niord.core.geomesa;

import org.geotools.data.Query;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.factory.Hints;
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes;
import org.niord.core.aton.AtonNode;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The implementation of the AtoN data entrie transported through the Geomesa
 * data stores.
 */
public class GeomesaAton implements GeomesaData<AtonNode>{

    private SimpleFeatureType sft = null;
    private List<SimpleFeature> features = null;
    private List<Query> queries = null;
    private Filter subsetFilter = null;

    @Override
    public String getTypeName() {
        return "AtoN";
    }

    @Override
    public SimpleFeatureType getSimpleFeatureType() {
        if (sft == null) {
            // list the attributes that constitute the feature type
            // this is a reduced set of the attributes from GDELT 2.0
            StringBuilder attributes = new StringBuilder();
            attributes.append("atonUID:String,");
            attributes.append("user:String,");
            attributes.append("timestamp:Date,");
            attributes.append("*geom:Point:srid=4326"); // the "*" denotes the default geometry (used for indexing)

            // create the simple-feature type - use the GeoMesa 'SimpleFeatureTypes' class for best compatibility
            // may also use geotools DataUtilities or SimpleFeatureTypeBuilder, but some features may not work
            sft = SimpleFeatureTypes.createType(getTypeName(), attributes.toString());

            // use the user-data (hints) to specify which date field to use for primary indexing
            // if not specified, the first date attribute (if any) will be used
            // could also use ':default=true' in the attribute specification string
            sft.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "atonUID");
        }
        return sft;
    }

    @Override
    public List<SimpleFeature> getFeatureData(List<AtonNode> atons) {
        if (features == null) {
            List<SimpleFeature> features = new ArrayList<>();

            // Use a geotools SimpleFeatureBuilder to create our features
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(getSimpleFeatureType());

            for(AtonNode aton: atons) {
                builder.set("atonUID", aton.getAtonUid());
                builder.set("user", aton.getUser());
                builder.set("timestamp", aton.getTimestamp());
                builder.set("geom", "POINT (" + aton.getLon() + " " + aton.getLat() + ")");

                // be sure to tell GeoTools explicitly that we want to use the ID we provided
                builder.featureUserData(Hints.USE_PROVIDED_FID, Boolean.TRUE);

                // build the feature - this also resets the feature builder for the next entry
                // use the AtoN UID as the feature ID
                features.add(builder.buildFeature(aton.getAtonUid()));
            }
            this.features = Collections.unmodifiableList(features);
        }
        return features;
    }

    @Override
    public List<Query> getFeatureQueries() {
        if (queries == null) {
            List<Query> queries = new ArrayList<>();
            // this data set is meant to show streaming updates over time, so just return all features
            queries.add(new Query(getTypeName(), Filter.INCLUDE));
            this.queries = Collections.unmodifiableList(queries);
        }
        return queries;
    }

    @Override
    public Filter getSubsetFilter() {
        return Filter.INCLUDE;
    }
}
