/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WSO2 LLC - support for WSO2 Micro Integrator Configuration
 */

package org.eclipse.lemminx.customservice.synapse.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies {@link Utils} hands out UI schemas copied per caller and mustache templates shared, with caches seeded directly here since test resources resolve to {@code file:} URLs the loaders can't read as a folder.
 */
public class ResourceCacheTest {

    private static final String FOLDER = "org/eclipse/lemminx/mediators/test";

    @BeforeEach
    @AfterEach
    void clearCaches() throws Exception {
        cache("UI_SCHEMA_CACHE").clear();
        cache("TEMPLATE_CACHE").clear();
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> cache(String name) throws Exception {
        Field field = Utils.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(null);
    }

    private static void seedSchema(String key, String value) throws Exception {
        JsonObject schema = new JsonObject();
        schema.addProperty("name", value);
        cache("UI_SCHEMA_CACHE").put(FOLDER, Map.of(key, schema));
    }

    @Test
    void eachCallerGetsItsOwnCopyOfASchema() throws Exception {
        seedSchema("log", "log");

        Map<String, JsonObject> first = Utils.getUISchemaMap(FOLDER);
        Map<String, JsonObject> second = Utils.getUISchemaMap(FOLDER);
        assertNotSame(first.get("log"), second.get("log"), "callers must not share a schema instance");

        first.get("log").addProperty("name", "mutated");

        assertEquals("log", second.get("log").get("name").getAsString(),
                "one caller's change must not be visible to another");
        assertEquals("log", Utils.getUISchemaMap(FOLDER).get("log").get("name").getAsString(),
                "one caller's change must not reach the cache");
    }

    @Test
    void compiledTemplatesAreSharedButTheMapIsNot() throws Exception {
        Mustache template = new DefaultMustacheFactory().compile(new StringReader("{{value}}"), "t");
        cache("TEMPLATE_CACHE").put(FOLDER, Map.of("t", template));

        Map<String, Mustache> first = Utils.getTemplateMap(FOLDER);
        Map<String, Mustache> second = Utils.getTemplateMap(FOLDER);

        assertNotSame(first, second, "each caller gets its own map");
        assertSame(first.get("t"), second.get("t"), "compiled templates are read-only and shared");
    }

    @Test
    void concurrentCallersDoNotSeeEachOther() throws Exception {
        seedSchema("log", "log");

        int callers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < callers; i++) {
                String marker = "caller-" + i;
                results.add(pool.submit(() -> {
                    start.await();
                    JsonObject mine = Utils.getUISchemaMap(FOLDER).get("log");
                    mine.addProperty("name", marker);
                    return mine.get("name").getAsString();
                }));
            }
            start.countDown();
            for (int i = 0; i < callers; i++) {
                assertEquals("caller-" + i, results.get(i).get(),
                        "a caller must keep the schema it was handed");
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals("log", Utils.getUISchemaMap(FOLDER).get("log").get("name").getAsString(),
                "the cached schema survives every caller unchanged");
    }
}
