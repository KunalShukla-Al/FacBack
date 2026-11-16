# Database integration checks

Application revision tested: da7decf. See docs/testing/integration-results.txt.

Use a NEW database named facback_integration_<unique suffix>. Copy schema.sql and replace only its initial CREATE DATABASE/USE statements with this database name. Do not run against your application database. Run from a temporary working directory so media fixtures cannot affect existing files.

Compile from repository root:

```sh
javac -d bin src/*.java src/dao/*.java src/logic/*.java src/ui/*.java tests/integration/IntegrationTests.java
```

Set FACBACK_DB_URL to jdbc:mysql://localhost:3306/<test database>, FACBACK_DB_USER, FACBACK_DB_PASSWORD and a test-only FACBACK_SECRET. Run IntegrationTests with absolute paths to bin and lib/mysql-connector-j-9.3.0.jar on the classpath. The harness refuses a database URL without /facback_integration_. Use a fresh schema on each run; fixtures intentionally remain for inspection.

The 14 assertions exercise real Java/JDBC methods, including rollback of an invalid review after its cooldown write. Media copy/discard is tested directly; this does not simulate the GUI's failed-save cleanup callback. This run tests a fresh schema, not the legacy SQL migration, concurrent submissions, or the complete GUI.
