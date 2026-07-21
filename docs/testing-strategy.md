# Strategie testování

## 1. Cíl

Testy mají chránit fyzické zboží, zákaznické výdeje a konzistenci skladové dostupnosti.

## 2. Jednotkové testy

Ověřit zejména:

- validaci zákaznického PINu,
- validaci servisního PINu,
- výběr boxu podle aktuálního obsahu a dostupnosti,
- změnu skladové dostupnosti po servisním doplnění,
- přechody stavů výdeje,
- expiraci,
- odmítnutí neplatných kombinací.

## 3. Integrační testy se SQLite

Použít skutečné SQLite schéma tam, kde záleží na:

- unikátních omezeních,
- cizích klíčích,
- transakcích,
- souběžném přidělování,
- SQL dotazech Spring JDBC.

Povinné scénáře:

1. dva požadavky soutěží o poslední volný box,
2. pouze jeden požadavek uspěje,
3. rollback nezanechá box v mezistavu,
4. quantity 2 přidělí dva různé boxy,
5. nedostatek boxů nevede k nekonzistentnímu částečnému přidělení.

## 4. MQTT kontraktní testy

Ověřit:

- správná témata,
- retained versus event zprávy,
- neplatný payload,
- duplicitní event,
- opožděný event,
- stav `OFFLINE`,
- korelaci příkazu a potvrzení,
- opakovaný `commandId`.

## 5. WooCommerce integrační scénáře

Ověřit:

- opakovaný webhook,
- zaplacená objednávka,
- nezaplacená objednávka,
- storno,
- quantity 2,
- souběžné objednávky,
- aktualizaci skladové dostupnosti,
- selhání mezi přijetím webhooku a přidělením boxu.

## 6. Testy bezpečnosti

Ověřit:

- PIN není v logu,
- čtyřmístný vstup neprojde servisní validací,
- šestimístný vstup neprojde zákaznickou validací,
- expirovaný výdej neotevře box,
- PIN jiného výdeje neotevře cílový box,
- neexistující lokace nebo box je odmítnut,
- chybějící potvrzení ESP není úspěch.

## 7. Testovací příkazy

Agent musí zjistit skutečné příkazy projektu. Výchozí očekávání:

```bash
./gradlew test
```

Podle projektu také:

```bash
./gradlew build
```

Do finálního shrnutí uvést, co bylo skutečně spuštěno.
