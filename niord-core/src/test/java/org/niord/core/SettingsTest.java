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
package org.niord.core;

import org.junit.Test;
import org.niord.core.settings.SettingValueExpander;

import static org.junit.Assert.assertEquals;

/**
 * Settings test
 */
public class SettingsTest {

    @Test
    public void testSettingValueExpander() {

        SettingValueExpander sve = new SettingValueExpander("${a}${b}${c}");
        assertEquals("a", sve.nextToken());
        sve.replaceToken(sve.nextToken(), "A");
        assertEquals("b", sve.nextToken());
        sve.replaceToken(sve.nextToken(), "B");
        assertEquals("c", sve.nextToken());
        sve.replaceToken(sve.nextToken(), "C");
        assertEquals("ABC", sve.getValue());

        // Test token nesting
        sve = new SettingValueExpander("xyz ${a} 123");
        sve.replaceToken(sve.nextToken(), "xxx${b}yyy");
        assertEquals("b", sve.nextToken());
    }
}
