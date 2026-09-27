# Kubling Hibernate Dialect

This module provides the Hibernate ORM dialect for Kubling and publishes a `DialectResolver` for automatic discovery.
The supported Hibernate version is managed centrally in the repository root `pom.xml`.

> [!NOTE]
> This module is deprecated as part of Kubling Core. It will be removed from this repository after being relocated to
> a dedicated Kubling repository for development tools for the Java ecosystem. The published artifact remains available
> during the transition.

Add the module to the application dependencies:

```xml
<dependency>
    <groupId>com.kubling</groupId>
    <artifactId>kubling-hibernate-dialect</artifactId>
    <version>${kubling.version}</version>
</dependency>
```

Hibernate can resolve the dialect from Kubling database metadata. It can also be configured explicitly with:

```properties
hibernate.dialect=com.kubling.hibernate.dialect.KublingDialect
```
