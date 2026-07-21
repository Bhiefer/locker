# ADR-004: Model fyzického stavu

- Status: Accepted

## Kontext

Zámek po odemknutí automaticky otevře dveře a hardware obsahuje senzor dveří.

## Rozhodnutí

Nevytvářet bez potřeby dvě samostatné doménové entity `lock` a `door`.

Primární fyzický stav boxu je stav dveří:

- `OPEN`,
- `CLOSED`,
- `UNKNOWN`,
- volitelně `FAULT`.

## Důsledky

Fyzický stav dveří je oddělen od obchodní dostupnosti boxu.
