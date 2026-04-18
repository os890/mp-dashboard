/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.os890.mp.dashboard;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

import jakarta.enterprise.inject.Instance;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/dashboard/api/health")
public class HealthResource {

    @Inject
    @Liveness
    private Instance<HealthCheck> livenessChecks;

    @Inject
    @Readiness
    private Instance<HealthCheck> readinessChecks;

    @Inject
    @Startup
    private Instance<HealthCheck> startupChecks;

    @Inject
    @ConfigProperty(name = "dashboard.health.management.port", defaultValue = "9990")
    private String managementPort;

    @Inject
    @ConfigProperty(name = "dashboard.health.management.host", defaultValue = "localhost")
    private String managementHost;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getHealth() {
        Map<String, String> categoryMap = buildCategoryMap();
        JsonArray serverChecks = fetchServerHealth();

        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("checks", enrichWithCategories(serverChecks, categoryMap));
        return result.build().toString();
    }

    private Map<String, String> buildCategoryMap() {
        Map<String, String> map = new HashMap<>();
        addCategories(map, livenessChecks);
        addCategories(map, readinessChecks);
        addCategories(map, startupChecks);
        return map;
    }

    private void addCategories(Map<String, String> map, Instance<HealthCheck> checks) {
        for (HealthCheck check : checks) {
            String name = check.call().getName();
            String category = resolveCategory(check);
            map.put(name, category);
        }
    }

    private String resolveCategory(HealthCheck check) {
        Class<?> clazz = check.getClass();
        DashboardCategory cat = clazz.getAnnotation(DashboardCategory.class);
        if (cat == null && clazz.getSuperclass() != null) {
            cat = clazz.getSuperclass().getAnnotation(DashboardCategory.class);
        }
        return cat != null ? cat.value() : "Application";
    }

    private JsonArray fetchServerHealth() {
        try {
            URI uri = new URI("http", null, managementHost,
                    Integer.parseInt(managementPort), "/health", null, null);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            // SmallRye Health returns 503 when any check is DOWN, but body still has JSON
            InputStream is = conn.getResponseCode() < 400
                    ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                return Json.createArrayBuilder().build();
            }
            try (JsonReader reader = Json.createReader(is)) {
                JsonObject health = reader.readObject();
                return health.getJsonArray("checks");
            }
        } catch (Exception e) {
            return Json.createArrayBuilder().build();
        }
    }

    private JsonArrayBuilder enrichWithCategories(JsonArray checks, Map<String, String> categoryMap) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (JsonValue val : checks) {
            JsonObject check = val.asJsonObject();
            String name = check.getString("name");
            String category = categoryMap.getOrDefault(name, "Server");

            JsonObjectBuilder enriched = Json.createObjectBuilder();
            enriched.add("name", name);
            enriched.add("status", check.getString("status"));
            enriched.add("category", category);

            if (check.containsKey("data")) {
                JsonObject data = check.getJsonObject("data");
                for (String key : data.keySet()) {
                    enriched.add(key, data.get(key));
                }
            }
            arr.add(enriched);
        }
        return arr;
    }
}
