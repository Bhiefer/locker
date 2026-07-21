# Architektura `cz.evans.locker`

## 1. Cíl

Jednoduchý middleware mezi WooCommerce, databází a fyzickými výdejními boxy.

Hlavní odpovědnosti:

- správa dostupnosti boxů,
- atomické přidělení boxu objednávce,
- správa výdejních oprávnění a PINů,
- příjem webhooků z WooCommerce,
- komunikace s ESP přes MQTT,
- audit provozních a hardwarových událostí,
- poskytování omezeného administračního přehledu.

## 2. Kontextový diagram

```text
WooCommerce
    |
    | webhook / API
    v
Locker middleware (Java 21, Spring, Spring JDBC)
    |
    +---- SQLite
    |
    +---- MQTT broker
              |
              +---- ESP lokace A
              +---- budoucí ESP dalších lokalit

Volitelně:
Home Assistant čte odvozené provozní stavy.
Tailscale/VPN propojuje vzdálené lokace.
```

## 3. Doporučené vrstvy

### Adapter: WooCommerce

- validace webhooku,
- převod externích dat na interní příkaz,
- idempotence podle ID události nebo objednávky,
- žádná přímá manipulace s GPIO nebo MQTT.

### Application

Use-cases, například:

- `ReserveBoxes`
- `AssignPickup`
- `ValidatePickupPin`
- `OpenAssignedBox`
- `ExpirePickup`
- `HandleDoorStateChanged`
- `ReconcileWooCommerceStock`

### Domain

- pravidla aktuálního obsahu boxů pro produkty A/B/C,
- validace PINu,
- stavy boxu a výdeje,
- zákaz dvojího přidělení,
- pravidla expirace.

### Persistence

- Spring JDBC repository,
- explicitní transakce,
- databázová omezení jako druhá obranná linie,
- žádné ORM.

### Adapter: MQTT

- publikování příkazů,
- příjem retained stavů a událostí,
- korelace příkazu a výsledku,
- idempotentní zpracování.

## 4. Doporučené entity

Názvy lze přizpůsobit existujícímu kódu, význam však musí zůstat zachován.

### `Location`

- `id`
- `code`
- `displayName`
- `status`
- `lastSeenAt`

### `Box`

- `id`
- `locationId`
- `number`
- `currentProductGroup`
- `businessStatus`
- `doorState`
- `lastDoorStateAt`

Unikátní omezení: `(location_id, number)`.

### `Pickup`

- `id`
- `orderId`
- `orderItemId`
- `boxId`
- `customerPin`
- `status`
- `validUntil`
- `createdAt`
- `completedAt`

Musí existovat ochrana proti více aktivním výdejům na jednom boxu.

### `ServiceRestock`

- `id`
- `locationId`
- `boxId`
- `productGroup`
- `previousProductGroup`
- `performedAt`
- `performedBy`

Servisní doplnění nebo změna obsahu boxu mění skladovou dostupnost produktu na dané lokaci a musí být auditovatelné.

### `HardwareCommand`

- `id`
- `correlationId`
- `locationId`
- `boxId`
- `type`
- `status`
- `requestedAt`
- `acknowledgedAt`
- `failureReason`

### `AuditEvent`

- `id`
- `eventType`
- `locationId`
- `boxId`
- `pickupId`
- `correlationId`
- `occurredAt`
- bezpečný payload bez celého PINu

## 5. Transakční hranice

Atomicky provádět minimálně:

- výběr a rezervaci boxu,
- přechod rezervace na aktivní výdej,
- uvolnění rezervace,
- změnu stavu výdeje při expiraci,
- zápis deduplikačního záznamu webhooku spolu s obchodní změnou.

Publikace MQTT nemá být slepě považována za součást databázové transakce.  
Použít stav příkazu a následné potvrzení nebo transakční outbox, pokud bude spolehlivost vyžadovat vyšší úroveň.

## 6. MQTT kontrakt

### Stav dveří

Topic:

```text
palivomat/<location>/box/<boxId>/door
```

Payload:

```text
OPEN
CLOSED
UNKNOWN
FAULT
```

Retain: ano.

### Stav lokace

Topic:

```text
palivomat/<location>/status
```

Payload:

```text
ONLINE
OFFLINE
```

Retain: ano. Použít LWT.

### Události

Topic:

```text
palivomat/<location>/event
```

Retain: ne.

Událost má obsahovat jedinečný identifikátor, čas, typ a relevantní box. Přesný JSON kontrakt musí být verzovaný, jakmile je zaveden.

### Příkazy

Doporučený namespace:

```text
palivomat/<location>/command
```

Příkaz musí obsahovat alespoň:

- jedinečný `commandId`,
- typ příkazu,
- cílový box,
- čas vytvoření,
- případnou expiraci příkazu.

ESP musí odmítnout starý nebo duplicitní příkaz podle definovaného pravidla.

## 7. Více lokalit

Architektura má od začátku obsahovat `locationId`, i když je nyní aktivní pouze jedna lokace.

Nesmí se předpokládat:

- globálně unikátní číslo boxu bez lokace,
- jedna pevná IP adresa ESP,
- jedna MQTT session bez identifikace lokace.

Pro vzdálené lokace se počítá s Tailscale/VPN. Veřejné vystavení interních služeb není preferováno.

## 8. Provozní profil

- přibližně 5 až 10 boxů na lokaci,
- aktuální MVP šest boxů,
- nízká četnost, řádově do 5 otevření denně,
- solární napájení: 3 × 100 W,
- baterie: LiFePO4 12,8 V,
- LTE router,
- systém musí bezpečně zvládat výpadky napájení a konektivity.

Nízký provoz neodůvodňuje obcházení atomických operací, validací nebo auditu.
