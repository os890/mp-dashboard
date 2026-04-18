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
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/dashboard/api/metrics")
public class MetricsResource {

    @Inject
    @ConfigProperty(name = "dashboard.health.management.port", defaultValue = "9990")
    private String managementPort;

    @Inject
    @ConfigProperty(name = "dashboard.health.management.host", defaultValue = "localhost")
    private String managementHost;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getMetrics() {
        Map<String, String> raw = fetchPrometheusMetrics();

        JsonObjectBuilder jvm = Json.createObjectBuilder();
        jvm.add("cpuAvailable", getVal(raw, "base_cpu_availableProcessors"));
        jvm.add("cpuLoad", getVal(raw, "base_cpu_processCpuLoad"));
        jvm.add("systemLoad", getVal(raw, "base_cpu_systemLoadAverage"));
        jvm.add("uptimeSeconds", getVal(raw, "base_jvm_uptime_seconds"));
        jvm.add("heapUsedMb", toMb(raw, "base_memory_usedHeap_bytes"));
        jvm.add("heapMaxMb", toMb(raw, "base_memory_maxHeap_bytes"));
        jvm.add("nonHeapUsedMb", toMb(raw, "base_memory_usedNonHeap_bytes"));
        jvm.add("threadCount", getVal(raw, "base_thread_count"));
        jvm.add("threadDaemon", getVal(raw, "base_thread_daemon_count"));
        jvm.add("threadPeak", getVal(raw, "base_thread_max_count"));
        jvm.add("classesLoaded", getVal(raw, "base_classloader_loadedClasses_count"));
        jvm.add("classesUnloaded", getVal(raw, "base_classloader_unloadedClasses_total"));

        JsonObjectBuilder gc = Json.createObjectBuilder();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            if (e.getKey().startsWith("base_gc_total{")) {
                String name = extractLabel(e.getKey());
                gc.add(name + " collections", toInt(e.getValue()));
            }
            if (e.getKey().startsWith("base_gc_time_total_seconds{")) {
                String name = extractLabel(e.getKey());
                gc.add(name + " time", toMs(e.getValue()));
            }
        }

        return Json.createObjectBuilder()
                .add("jvm", jvm)
                .add("gc", gc)
                .build().toString();
    }

    private String toInt(String val) {
        try {
            return String.valueOf((long) Double.parseDouble(val));
        } catch (NumberFormatException e) {
            return val;
        }
    }

    private String toMs(String seconds) {
        try {
            double s = Double.parseDouble(seconds);
            if (s < 0.001) return "0 ms";
            if (s < 1.0) return String.format("%.0f ms", s * 1000);
            return String.format("%.2f s", s);
        } catch (NumberFormatException e) {
            return seconds;
        }
    }

    private String getVal(Map<String, String> raw, String key) {
        return raw.getOrDefault(key, "n/a");
    }

    private String toMb(Map<String, String> raw, String key) {
        String val = raw.get(key);
        if (val == null) return "n/a";
        try {
            return String.format("%.1f", Double.parseDouble(val) / (1024 * 1024));
        } catch (NumberFormatException e) {
            return val;
        }
    }

    private String extractLabel(String metric) {
        int start = metric.indexOf("name=\"");
        if (start < 0) return "unknown";
        start += 6;
        int end = metric.indexOf("\"", start);
        return end > start ? metric.substring(start, end) : "unknown";
    }

    private Map<String, String> fetchPrometheusMetrics() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            URI uri = new URI("http", null, managementHost,
                    Integer.parseInt(managementPort), "/metrics", null, null);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            InputStream is = conn.getResponseCode() < 400
                    ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return result;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    int space = line.lastIndexOf(' ');
                    if (space > 0) {
                        result.put(line.substring(0, space), line.substring(space + 1));
                    }
                }
            }
        } catch (Exception e) {
            // management port not reachable
        }
        return result;
    }
}
