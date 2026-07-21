# ADR-006: Vývoj databázového schématu

- Status: Provisional

## Kontext

V raném MVP bylo požadováno provádět SQL změny pouze úpravou inicializačního `CREATE DB` skriptu.

## Rozhodnutí

Pro čistě lokální vývoj s vždy novou databází lze dočasně upravovat inicializační schéma.

Jakmile existují zachovávaná provozní data, musí každá změna schématu mít explicitní migrační postup.

## Důsledky

Agent musí před změnou schématu zjistit, zda se pracuje s jednorázově vytvářenou databází, nebo s existujícím nasazením. Nesmí automaticky smazat databázi.
