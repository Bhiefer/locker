# ADR-001: Technologický základ

- Status: Accepted
- Rozhodnutí: Java 21, Gradle 8.7, Spring JDBC a SQLite
- Datum kontextu: 2025–2026

## Kontext

Projekt má být jednoduchý middleware pro malý počet boxů a nízkou provozní zátěž. Uživatel zná Javu a nechce zbytečně složitou infrastrukturu.

## Rozhodnutí

Použít:

- Java 21,
- Gradle 8.7,
- Spring,
- Spring JDBC,
- SQLite.

## Důsledky

- SQL zůstává explicitní,
- není použito ORM,
- databázová omezení jsou důležitou součástí ochrany domény,
- změna databáze nebo persistence frameworku vyžaduje nové rozhodnutí.
