# Validation performed in this review package

Performed before packaging:

- parsed all Java source files with the JDK 21 javac parser: **0 syntax errors**;
- verified every internal `com.greenink.api.*` import points to a source type in this repository;
- parsed `pom.xml` as XML;
- parsed both YAML configuration files;
- parsed `catalog.json` and verified **6 units / 200 chapters / 24 free chapters**;
- parsed `pyq-metadata.json` and verified **6,099 total prototype PYQs**.

The execution environment used to prepare this package does not have Maven installed and cannot resolve Maven Central from the container, so a full dependency compile and `mvn test` could not be executed here. The repository includes `ApiFlowIntegrationTest`; the developer should make `mvn test` the first review command on a normal networked development machine.
