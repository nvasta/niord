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
package org.niord.core.aton;

/**
 * Defines the two IALA Buoyage Systems
 */
public enum  IalaBuoyageSystem {
    IALA_A("IALA-A"),
    IALA_B("IALA-B");

    private String name;

    /** Constructor */
    IalaBuoyageSystem(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

    /** Returns the other IALA system */
    IalaBuoyageSystem other() {
        return this == IALA_A ? IALA_B : IALA_A;
    }
}
