# ADR-005: MQTT stavy a události

- Status: Accepted

## Rozhodnutí

Retained stavová témata:

- `palivomat/<location>/box/<boxId>/door`
- `palivomat/<location>/status`

Neretained událost:

- `palivomat/<location>/event`

Stav lokace používá `ONLINE/OFFLINE` a Last Will.

## Důsledky

Middleware může po připojení obnovit poslední známý stav, ale musí pracovat s časem a stářím informace. Události musí být idempotentní.
