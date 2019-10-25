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
package org.niord.model.geojson;

import io.swagger.annotations.ApiModel;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.function.Consumer;

/**
 * GeoJSON MultiPoint, as defined in the specification:
 * http://geojson.org/geojson-spec.html#multipoint
 */
@ApiModel(
        value = "MultiPoint",
        parent = GeoJsonVo.class,
        description = "GeoJson MultiPoint type"
)
@XmlRootElement(name = "multiPoint")
public class MultiPointVo extends GeometryVo {

    private double[][] coordinates;

    /**
     * Instantiates a new Multi point vo.
     */
    @SuppressWarnings("unused")
    public MultiPointVo() {
        setType("MultiPoint");
    }

    /**
     * Instantiates a new Multi point vo.
     *
     * @param coordinates the coordinates
     */
    public MultiPointVo(double[][] coordinates) {
        this();
        this.coordinates = coordinates;
    }

    /** {@inheritDoc} */
    @Override
    public void visitCoordinates(Consumer<double[]> handler) {
        visitCoordinates(coordinates, handler);
    }

    /**
     * Get coordinates double [ ] [ ].
     *
     * @return the double [ ] [ ]
     */
    public double[][] getCoordinates() {
        return coordinates;
    }

    /**
     * Sets coordinates.
     *
     * @param coordinates the coordinates
     */
    public void setCoordinates(double[][] coordinates) {
        this.coordinates = coordinates;
    }
}

