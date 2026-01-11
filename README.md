# Badge Service

The Badge Service manages user logbook entries for hiking achievements within the HIKU hiking application, allowing users to record their peak ascents and track their hiking history. It enriches logbook entries with peak metadata (name, elevation, territory) by communicating with the Peaks-Hikes Service via gRPC, providing a seamless experience for tracking completed hikes. The service is secured with JWT authentication via Keycloak and stores all logbook data in PostgreSQL.

For full documentation see: [Docs](./documentation.md)
