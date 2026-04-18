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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Path("/dashboard")
public class DashboardService {

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response redirect() {
        URI fw = uriInfo.getRequestUriBuilder().path("index.html").build();
        return Response.temporaryRedirect(fw).build();
    }

    @GET
    @Path("index.html")
    @Produces(MediaType.TEXT_HTML)
    public Response dashboard() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("META-INF/resources/dashboard/index.html")) {
            if (is == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            String html = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            return Response.ok(html, MediaType.TEXT_HTML)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .build();
        } catch (IOException e) {
            return Response.serverError().build();
        }
    }
}
