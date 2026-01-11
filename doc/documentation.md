# Badge Service - Technical Documentation

## Overview

The **Badge Service** is a microservice responsible for managing user logbook entries and digital hiking achievements within the HIKU hiking application. It provides REST endpoints for users to record their peak ascents and retrieves enriched peak metadata (name, elevation, territory) from the Peaks-Hikes Service via gRPC. The service acts as a gRPC client, demonstrating inter-service communication patterns in a microservices architecture.

## Table of Contents

1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Database Schema](#database-schema)
4. [gRPC Client Integration](#grpc-client-integration)
5. [Authentication & Authorization](#authentication--authorization)
7. [Configuration](#configuration)
8. [Deployment](#deployment)
9. [Local Development](#local-development)
10. [Error Handling](#error-handling)
11. [Troubleshooting](#troubleshooting)

---

## Architecture

### Key Components

- **REST Controllers**: Handle HTTP requests for logbook management (LogbookController, HealthController, Hello)
- **Logbook Service**: Business logic for managing user hiking achievements and logbook entries
- **gRPC Client**: PeakServiceClient communicates with Peaks-Hikes Service to enrich logbook entries with peak metadata
- **Database Layer**: JPA entities and services for persisting logbook entries
- **JWT Authentication**: All endpoints require authenticated users via Keycloak integration

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Runtime** | Java (Eclipse Temurin) | 21 |
| **Build Tool** | Maven | 3.9 |
| **Framework** | KumuluzEE | 4.1.0 |
| **JPA Provider** | Hibernate | 5.6.15.Final |
| **Database** | PostgreSQL | 14+ |
| **Authentication** | MicroProfile JWT | 2.1 |
| **gRPC Client** | gRPC Java | 1.58.0 |
| **Protobuf** | Protocol Buffers | 3.x |
| **Containerization** | Docker | - |
| **Orchestration** | Kubernetes (via Helm) | - |


## Database Schema

**Schema**: `badge_service`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | serial | Primary key | Surrogate key for logbook entries |
| user_id | varchar(255) | Not null | Keycloak user ID (changed from int in V2 migration) |
| peak_id | int | Not null | Peak identifier from Peaks-Hikes Service |
| added_at | timestamp | Not null; default `now()` | Entry creation timestamp |
| notes | text | Nullable | Free-form notes for the ascent |

**Indexes/Constraints**: Primary key on `id`; consider an additional index on `(user_id, peak_id)` to speed up user logbook lookups if query volume grows.



## gRPC Client Integration

### Sending gRPC Requests to Peaks-Hikes Service

The Badge Service acts as a **gRPC client**, consuming the PeakService exposed by Peaks-Hikes Service to enrich user logbook entries with detailed peak information.

#### Architecture Components

1. **Shared Protobuffer Contract** — Badge Service compiles the same `peak_service.proto` schema as Peaks-Hikes Service, ensuring type-safe communication. The contract defines `GetPeakById` RPC method and `PeakResponse` message structure.

2. **PeakServiceClient** — Application-scoped CDI bean that initializes on startup:
   - Creates a `ManagedChannel` to Peaks-Hikes Service using environment variables (`PEAKS_GRPC_HOST`, `PEAKS_GRPC_PORT`)
   - Instantiates a `PeakServiceBlockingStub` for synchronous RPC calls
   - Handles connection lifecycle and graceful shutdown

3. **Request Flow** — When displaying user logbooks:
   - LogbookController retrieves logbook entries (containing only peak IDs)
   - For each entry, `peakServiceClient.getPeakById(peakId)` sends a gRPC request
   - Blocking call waits for `PeakResponse{name, territory, elevation_m, found}`
   - Peak metadata enriches the logbook DTO returned to the frontend

#### Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                     Badge Service                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ User requests "Your Logbook":                           │ │
│  │  1. LogbookController.list(userId)                      │ │
│  │  2. LogbookService returns entries (peak IDs only)      │ │
│  │  3. For each peak_id, invoke PeakServiceClient          │ │
│  │  4. Blocking gRPC call: getPeakById(peak_id)            │ │
│  │  5. Receive PeakResponse with metadata                  │ │
│  │  6. Enrich LogbookEntryDto and return to client         │ │
│  └─────────────────────────────────────────────────────────┘ │
│            │                                                 │
│            │ gRPC Request (HTTP/2 binary)                    │
│            │ PeakRequest{peak_id: 5}                         │
│            ▼                                                 │
└──────────────────────────────────────────────────────────────┘
                      Network (port 9090)
┌──────────────────────────────────────────────────────────────┐
│                  Peaks-Hikes Service                         │
│            ▲                                                 │
│            │ gRPC Response (HTTP/2 binary)                   │
│            │ PeakResponse{id, name, territory, elevation_m}  │
│            │                                                 │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ GrpcServer (port 9090) handles request:                 │ │
│  │  - PeakServiceImpl queries database                     │ │
│  │  - Returns peak metadata                                │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

#### Key Client Implementation Details

**Environment-Based Service Discovery:**
- Reads `PEAKS_GRPC_HOST` and `PEAKS_GRPC_PORT` environment variables
- Defaults to `peaks-hikes-service:9090` for Kubernetes service discovery

**Channel Initialization:**
- Uses `ManagedChannelBuilder.forAddress()` with plaintext (no TLS in dev)
- Channel persists for application lifetime, reused across requests
- HTTP/2 multiplexing allows concurrent peak lookups without blocking

**Synchronous Blocking Stub:**
- Thread blocks until response received
- Simplifies controller logic (no async callbacks)
- Timeout configured via channel settings

**Resilience & Fallback:**
- On gRPC failure, returns partial response with `found=false`
- Prevents logbook display failures due to Peaks-Hikes downtime
- Logs errors for monitoring

**Benefits of gRPC Client:**
- **Type Safety**: Protobuf prevents runtime serialization errors
- **Performance**: Binary protocol faster than JSON over HTTP/1.1
- **Versioning**: Schema evolution with backward compatibility
- **Kubernetes Integration**: Service discovery via environment variables enables pod-to-pod networking

## Authentication & Authorization

### JWT-Based Authentication

The Badge Service uses **MicroProfile JWT** with Keycloak as the identity provider. All REST endpoints require `@RolesAllowed("user")` for authenticated access. Users can only access their own logbook entries.

## Deployment

#### Database Migrator Image

**Dockerfile**: `Dockerfile.migrator`

Runs Flyway migrations as a Kubernetes Job.

---

### Kubernetes (Helm)

#### Chart Structure

```
helm/
├── Chart.yaml              # Chart metadata
├── values-dev.yaml         # Development values
└── templates/
    ├── _helpers.tpl        # Template helpers
    ├── deployment.yaml     # Main application deployment
    ├── service-clusterip.yaml  # Internal service
    ├── service-nodeport.yaml   # External service (dev)
    ├── migrate-job.yaml    # Database migration job
    ├── secret.yaml         # Database credentials
    └── secretsproviderclass.yaml  # Azure Key Vault integration
```

---

### CI/CD Pipelines

#### Test Environment Pipeline

**File**: `.github/workflows/test-build-deploy.yaml`

**Triggers**:
- Push to `test` branch

**Steps**:
1. Checkout code
2. Build Docker images (app + migrator)
3. Push to Azure Container Registry (ACR)
4. Deploy to AKS test environment using ArgoCD

---

#### Production Promotion Pipeline

**File**: `.github/workflows/prod-promote.yaml`

**Triggers**:
- Manual workflow dispatch with image tag selection

**Steps**:
1. Pull images from test ACR
2. Retag images for production
3. Push to production ACR
4. Deploy to AKS production environment

Secrets used in the GitHub Actions workflows are saved as secrets in our GitHub Organization. Secrets used for deployment on the Azure cluster are provided by our Azure Key Vault.

---

## Local Development

Building images and deployment for local development is handled by Skaffold. By running the command **skaffold dev** in the root folder of the repository in a terminal window will make Skaffold automatically build and deploy the service to your local Minikube cluster. Skaffold watches your local files and when you save a change, Skaffold automatically applies it.  

### Prerequisites

#### Required Tools & Services
- **Java 21**
- **Maven 3.9+**
- **PostgreSQL 14+**
- **Docker Desktop**
- **Keycloak**
- **RabbitMQ**
- **Minikube**
- **Skaffold**

#### Other requirements
- Docker Desktop is running,
- Minikube cluster is running on Docker Desktop,
- The database is deployed on your local cluster,
- The Traefik ingress controller is deployed on your local cluster,
- Keycloak is deployed on your local cluster.

### Steps performed by Skaffold
- Builds docker image for microservice,
- Builds docker image for database migrations,
- Deploys both images,
- Portforwards NodePort to the default port setting.

## Error Handling

### Common HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| `200 OK` | Request successful | GET logbook, POST logbook entry |
| `201 Created` | Resource created | - |
| `204 No Content` | Success, no response body | - |
| `400 Bad Request` | Invalid request data | Missing userId or peakId |
| `401 Unauthorized` | Missing or invalid JWT | No Authorization header |
| `403 Forbidden` | Insufficient permissions | Access another user's logbook |
| `404 Not Found` | Resource not found | Peak/trail doesn't exist |
| `500 Internal Server Error` | Server error | Database connection failed |
| `503 Service Unavailable` | Service unhealthy | Health check failed |

### Exception Handling

The service uses JAX-RS exception handling:

- **Validation errors**: Return `400 Bad Request`
- **Resource not found**: Return `404 Not Found` (explicit in delete)
- **Database errors**: Return `500 Internal Server Error`
- **Authentication errors**: Return `401 Unauthorized`




## Contact

For questions or issues, contact the development team.


**Last Updated**: January 11, 2026  
**Version**: 0.1.0
