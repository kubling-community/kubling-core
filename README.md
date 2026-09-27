# Kubling Core

[![Kubling license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

Kubling Core contains the Java types, serialization support, JDBC driver, and integration modules shared by Kubling
components. Kubling uses gRPC for language-neutral integrations; JVM applications can also use the Java client for
Kubling's native database protocol, which provides JDBC access.

## Modules

| Module | Artifact | Purpose |
| --- | --- | --- |
| `common-core` | `com.kubling:kubling-common-core` | Shared runtime types, conversions, LOB support, serialization, and core utilities. |
| `java-native-protocol-client` | `com.kubling:kubling-client` | Java implementation of Kubling's native database protocol, including JDBC, XA, authentication, socket transport, requests, results, and metadata. |
| `hibernate-dialect` | `com.kubling:kubling-hibernate-dialect` | Deprecated in Kubling Core; scheduled to move to a dedicated repository for Kubling development tools for the Java ecosystem. |
| `test-container` | `com.kubling:kubling-test-container` | Testcontainers integration for Java applications. |
| `build` | `com.kubling:kubling` | JDBC and source distribution assemblies. |

## Building from source

Published artifacts target Java 21. Use the included Maven Wrapper to build the project and run its tests:

```bash
./mvnw verify
```

The standalone JDBC driver can be built with:

```bash
./mvnw -Pdriver-release package
```

## Versioning

This repository has its own release cycle. Artifact versions do not need to match the Kubling server version; each
server release declares the exact versions it consumes.

## License

Licensed under the [Apache License 2.0](LICENSE). See [COPYRIGHT.txt](COPYRIGHT.txt) for upstream attribution.
