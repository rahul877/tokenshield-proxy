# TokenShield Proxy 🛡️

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Reactive](https://img.shields.io/badge/Project%20Reactor-WebFlux-blue.svg?style=flat-square)](https://projectreactor.io/)
[![Virtual Threads](https://img.shields.io/badge/Project%20Loom-Virtual%20Threads%20Enabled-purple.svg?style=flat-square)](https://openjdk.org/projects/loom/)
[![R2DBC PostgreSQL](https://img.shields.io/badge/R2DBC-PostgreSQL-336791.svg?style=flat-square&logo=postgresql)](https://r2dbc.io/)
[![Reactive Redis](https://img.shields.io/badge/Redis-Reactive-DC382D.svg?style=flat-square&logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

**TokenShield Proxy** is a high-performance, non-blocking reactive API proxy, token manager, and rate-limiting shield. Designed for modern microservices and edge architectures, TokenShield combines the reactive power of **Spring WebFlux** and **Project Reactor** with the concurrency throughput of **Java 21 Virtual Threads (Project Loom)**, backed by **Reactive Redis** and **R2DBC PostgreSQL**.

---

## ⚡ Key Architectural Features

- **Java 21 & Spring Boot 3.3.x**: Leveraging the modern Java LTS platform features, pattern matching, record patterns, and modern Spring 3 framework optimizations.
- **Project Loom Virtual Threads**: Configured with `spring.threads.virtual.enabled: true` for lightweight, non-blocking thread scheduling and handling concurrent workloads with minimal footprint.
- **Spring WebFlux**: Fully reactive, event-driven HTTP server powered by Project Reactor and Netty.
- **Reactive Redis**: High-throughput distributed cache and token bucket rate limiter using Spring Data Redis Reactive.
- **R2DBC PostgreSQL**: Pure non-blocking reactive relational database connectivity without blocking JDBC drivers or thread starvation.
- **Cloud-Ready Observability**: Built-in Spring Boot Actuator with readiness/liveness health probes and Prometheus metrics.

---

## 🏗️ Architecture Overview

```
                      +-----------------------------+
                      |       Client Requests       |
                      +--------------+--------------+
                                     |
                                     v
                      +-----------------------------+
                      |      Spring WebFlux         |
                      |   Netty + Virtual Threads   |
                      +--------------+--------------+
                                     |
           +-------------------------+-------------------------+
           |                                                   |
           v                                                   v
+-------------------------+                         +-------------------------+
|   Reactive Redis Cache  |                         |    R2DBC PostgreSQL     |
| (Rate Limiting, Tokens) |                         |  (Persistent Metadata)  |
+-------------------------+                         +-------------------------+
           |                                                   |
           +-------------------------+-------------------------+
                                     |
                                     v
                      +-----------------------------+
                      |     Target Upstream API     |
                      +-----------------------------+
```

---

## 📋 Prerequisites

Before running the project, ensure you have the following installed:

- **JDK 21** or later ([Eclipse Temurin](https://adoptium.net/), [GraalVM](https://www.graalvm.org/), or OpenJDK)
- **Maven 3.9+** (or use an IDE with bundled Maven)
- **Docker** and **Docker Compose** (for running Redis and PostgreSQL locally)

---

## 🚀 Getting Started

### 1. Clone Repository

```bash
git clone https://github.com/rahul877/tokenshield-proxy.git
cd tokenshield-proxy
```

### 2. Start PostgreSQL & Redis

Use the included `docker-compose.yml` to spin up local infrastructure:

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16**: Port `5432` (`postgres`/`postgres`, database `tokenshield`)
- **Redis 7**: Port `6379`

### 3. Build the Application

```bash
mvn clean package
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on port `8080`.

---

## ⚙️ Configuration Reference

Application configuration is managed in `src/main/resources/application.yml`. Key environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | HTTP Server port |
| `SPRING_THREADS_VIRTUAL_ENABLED` | `true` | Enables Java 21 Project Loom virtual threads |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `tokenshield` | PostgreSQL database name |
| `DB_USER` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis server host |
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_PASSWORD` | *(empty)* | Redis authentication password |

---

## 🔍 Observability & Actuator

The application exposes standard Spring Boot Actuator endpoints for health and metrics:

- **Health Probe**: `GET http://localhost:8080/actuator/health`
- **Liveness**: `GET http://localhost:8080/actuator/health/liveness`
- **Readiness**: `GET http://localhost:8080/actuator/health/readiness`
- **Metrics**: `GET http://localhost:8080/actuator/metrics`
- **Prometheus**: `GET http://localhost:8080/actuator/prometheus`

---

## 📂 Project Structure

```
tokenshield-proxy/
├── src/
│   ├── main/
│   │   ├── java/com/tokenshield/proxy/
│   │   │   └── TokenShieldProxyApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/tokenshield/proxy/
│           └── TokenShieldProxyApplicationTests.java
├── .gitignore
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 📄 License

This project is licensed under the Apache 2.0 License.
