# MicroProfile Dashboard Example

Demonstrates the [mp-dashboard-addon](../README.md) with custom health checks, config properties, and WildFly 39 deployment.

## What's included

- **HelloResource** — simple REST endpoint at `/hello`
- **DatabaseHealthCheck** — `@Readiness` check with `@DashboardCategory("Database")`
- **MessagingHealthCheck** — `@Liveness` check with `@DashboardCategory("Messaging")`
- **StorageHealthCheck** — `@Liveness` + `@Readiness` check with `@DashboardCategory("Storage")`
- **Custom config properties** — `app.name`, `app.database.url`, `app.kafka.*`, etc.

## Build and Run

```bash
# From the project root
mvn clean install

# Run via Podman (from the example/ directory)
cd example
podman build -t dashboard-demo .
podman run --rm -p 8080:8080 dashboard-demo
```

## Endpoints

- REST: `http://localhost:8080/dashboard-demo/hello`
- Dashboard: `http://localhost:8080/dashboard-demo/dashboard`
- Health API: `http://localhost:8080/dashboard-demo/dashboard/api/health`
- Metrics API: `http://localhost:8080/dashboard-demo/dashboard/api/metrics`
- Config API: `http://localhost:8080/dashboard-demo/dashboard/api/config`

## Dashboard Features Demonstrated

- **Health tab** — Server checks (WildFly built-in) + Database, Messaging, Storage categories
- **Metrics cards** — CPU, memory, threads, GC stats from WildFly management port
- **Sparkline charts** — CPU load, heap memory, threads history (30 min, polled every 10s)
- **Config table** — filter by `app.` to see the custom properties
- **Dark/Light mode** — toggle in the header
