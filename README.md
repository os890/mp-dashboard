# MicroProfile Dashboard Addon

A self-contained dashboard addon for Jakarta EE 11 / MicroProfile applications on WildFly.
Provides a single HTML page with live health checks, JVM metrics with sparkline history, and a searchable MicroProfile Config viewer.

## Features

- **Health Checks** — shows all MicroProfile Health checks (server + app) with dynamic category grouping via `@DashboardCategory`
- **JVM Metrics** — CPU, memory (with heap progress bar), threads, classloader, GC stats from WildFly's Prometheus metrics endpoint
- **Metrics History** — CSS sparkline charts for CPU load, heap memory, and thread count (30 min, polled every 10s)
- **MicroProfile Config** — searchable table of all active config properties with sensitive value masking
- **Dark/Light mode** — toggle with localStorage persistence
- **Project-stage gating** — `@PreMatching` filter disables the dashboard based on `dashboard.enabled` or `project.stage` config
- **Self-contained** — single JAR, no external dependencies, no CDN requests

## Maven Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>mp-dashboard-addon</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

Add the dependency to your WAR project. The dashboard auto-discovers via JAX-RS when the app uses `@ApplicationPath("/")`.

Access the dashboard at: `http://localhost:8080/<context-root>/dashboard`

### Custom Health Check Categories

Annotate your MicroProfile Health checks with `@DashboardCategory` to group them in the dashboard:

```java
import org.os890.mp.dashboard.DashboardCategory;

@Readiness
@ApplicationScoped
@DashboardCategory("Database")
public class DatabaseHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("PostgreSQL Connection")
                .up()
                .withData("pool-size", "10")
                .build();
    }
}
```

Checks without `@DashboardCategory` from the app get category "Application". WildFly built-in checks get category "Server".

### Project-Stage Gating

The dashboard is controlled by two config properties:

- `dashboard.enabled` — explicit `true`/`false` override (takes precedence)
- `project.stage` — if set to `production` and `dashboard.enabled` is not set, the dashboard is disabled

```bash
# Enable dashboard
standalone.sh -Dproject.stage=development

# Disable dashboard (default when no config is set)
standalone.sh
```

### Management Port

The health and metrics APIs fetch data from WildFly's management interface. Configurable via:

```properties
dashboard.health.management.host=localhost
dashboard.health.management.port=9990
```

## Dashboard Sections

### Health Checks
- Category filter tabs: **Server** (default), then app categories alphabetically
- Status badges per category (ALL UP / ISSUES)
- Extra data fields shown per check

### JVM Metrics
- **CPU & Runtime** — processors, CPU load %, system load, uptime
- **Memory** — heap used/max with visual progress bar, non-heap
- **Threads & Classes** — active, daemon, peak threads; loaded/unloaded classes
- **Garbage Collection** — collections count and time per collector

### Metrics History
- Sparkline bar charts for CPU load, heap memory, and threads
- Polls every 10 seconds, keeps 30 minutes of data (180 points)
- Color-coded: normal (accent), warning (yellow), critical (red)

### MicroProfile Config
- Searchable filter across property names and values
- "Showing X of Y" counter with scroll hint
- Sensitive values (password, secret, token, credential) are masked

## License

Apache License, Version 2.0
