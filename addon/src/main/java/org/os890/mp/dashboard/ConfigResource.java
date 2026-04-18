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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.Config;

import java.util.TreeSet;

@Path("/dashboard/api/config")
public class ConfigResource {

    @Inject
    private Config config;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfig() {
        JsonArrayBuilder entries = Json.createArrayBuilder();
        TreeSet<String> sortedKeys = new TreeSet<>();

        for (String key : config.getPropertyNames()) {
            sortedKeys.add(key);
        }

        for (String key : sortedKeys) {
            JsonObjectBuilder entry = Json.createObjectBuilder();
            entry.add("key", key);
            try {
                String value = config.getValue(key, String.class);
                if (isSensitive(key)) {
                    entry.add("value", "******");
                } else {
                    entry.add("value", value);
                }
            } catch (Exception e) {
                entry.add("value", "<unresolvable>");
            }
            entries.add(entry);
        }

        return Json.createObjectBuilder()
                .add("entries", entries)
                .build()
                .toString();
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("secret")
                || lower.contains("credential") || lower.contains("token")
                || lower.contains("key") && !lower.contains("keystore");
    }
}
