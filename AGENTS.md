# AGENTS.md — závazná pravidla pro agenty

## 1. Účel

Tento repozitář obsahuje middleware projektu `cz.evans.locker` pro samoobslužné výdejní boxy.

Tento soubor je závazný pro všechny automatizované agenty, včetně Codexu.  
Při rozporu platí následující pořadí autority:

1. výslovný pokyn uživatele v aktuálním vlákně,
2. tento soubor `AGENTS.md`,
3. dokumenty v `docs/decisions/` se stavem `Accepted`,
4. `docs/domain-rules.md`,
5. `docs/architecture.md`,
6. existující automatizované testy,
7. současná implementace,
8. historické poznámky a chatové přepisy.

Při zjištění rozporu agent nesmí rozpor tiše vyřešit odhadem. Musí jej uvést ve shrnutí změn.

---

## 2. Povinný pracovní postup

Před změnou kódu agent musí:

1. přečíst tento soubor,
2. přečíst relevantní dokumenty v `docs/`,
3. projít související produkční kód a testy,
4. stručně formulovat předpoklady,
5. u změn přes více komponent nejprve vytvořit plán.

Po změně agent musí:

1. spustit relevantní testy,
2. pokud je to přiměřené, spustit celý testovací balík,
3. uvést přesné příkazy a jejich výsledky,
4. zkontrolovat Git diff,
5. uvést neověřené části a rizika.

Agent nesmí tvrdit, že něco funguje, pokud to neověřil testem, buildem nebo přímou kontrolou.

---

## 3. Technologická omezení

Aktuální technologický základ:

- Java 21
- Gradle 8.7
- Spring
- Spring JDBC
- SQLite
- jedno ESP zařízení na jednu lokaci
- komunikace s hardwarem přes MQTT
- budoucí propojení více lokalit přes Tailscale nebo jinou VPN

Zakázáno bez výslovného souhlasu uživatele:

- přidat Hibernate, JPA nebo jiné ORM,
- změnit SQLite za jinou databázi,
- zavést nový aplikační framework,
- přepsat projekt do jiného programovacího jazyka,
- zavést distribuovanou architekturu, message broker navíc nebo Kubernetes,
- přidat složité administrační UI,
- ukládat nové tajné údaje přímo do repozitáře.

Preferováno:

- co nejjednodušší moderní řešení,
- malý počet závislostí,
- explicitní SQL,
- čitelné doménové názvy,
- deterministické chování,
- testovatelnost bez fyzického hardwaru.

---

## 4. Závazná doménová pravidla

### 4.1 Boxy a produkty

Aktuální MVP používá šest boxů a tři produktové skupiny.

Produktová skupina není trvale svázaná s číslem boxu.

Při konstrukci nebo servisním doplnění každého výdejního místa se určuje, který konkrétní box aktuálně obsahuje kterou produktovou skupinu.

Například jedna lokace může mít v daném okamžiku:

- box 1 → produkt A,
- box 2 → produkt C,
- box 3 → produkt A,
- box 4 → produkt B,
- box 5 → prázdný,
- box 6 → mimo provoz.

Každý fyzický box obsahuje nejvýše jeden kus zboží.

Jeden prodaný kus musí být přiřazen právě jednomu konkrétnímu boxu.

Při objednání množství `2` musí vzniknout dvě samostatná přiřazení a zákazník musí obdržet dvě samostatné sady údajů k vyzvednutí.

Sklad WooCommerce má odpovídat počtu boxů, které jsou pro příslušný produkt na dané lokaci aktuálně naplněné, obchodně dostupné a nejsou rezervované, přidělené ani mimo provoz.

### 4.2 PIN

- zákaznický PIN je čtyřmístný a pouze číselný,
- servisní PIN je šestimístný a pouze číselný,
- zákaznický PIN smí otevřít pouze box přiřazený konkrétnímu výdeji,
- servisní PIN nesmí být zaměněn za zákaznický PIN,
- PIN se v současném MVP ukládá čitelně; agent jej nesmí jednostranně převést na hash bez změny požadavků a migračního plánu.

Nikdy nelogovat PIN v běžném informačním logu.  
Pokud je nutné PIN identifikovat v diagnostice, použít maskování nebo interní identifikátor výdeje.

### 4.3 Výdej

- standardní lhůta k vyzvednutí je 3 dny,
- po jejím uplynutí má být zboží řešeno osobním vyzvednutím na prodejně,
- platba probíhá prostřednictvím WooCommerce platební brány,
- systém musí rozlišovat alespoň volný, rezervovaný a obsazený/přidělený box,
- fyzický stav dveří nesmí být automaticky považován za obchodní stav boxu.

---

## 5. Hardware a stav dveří

Elektrické zámky mají senzory dveří. Odemknutí zámku fyzicky způsobí otevření dveří.

Model nesmí bez potřeby vytvářet samostatné doménové entity `lock` a `door`.  
Pro fyzický stav boxu je primární stav dveří.

Minimální podporované fyzické stavy:

- `OPEN`
- `CLOSED`
- `UNKNOWN`

Doporučený diagnostický stav:

- `FAULT`

ESP musí posílat každou změnu stavu do middleware.  
Middleware je autoritativní pro obchodní logiku a audit; ESP je autoritativní pouze pro bezprostředně měřený hardwarový stav.

---

## 6. MQTT pravidla

Preferovaná struktura témat:

- `palivomat/<location>/box/<boxId>/door`
- `palivomat/<location>/status`
- `palivomat/<location>/event`

Požadované chování:

- stav dveří `OPEN/CLOSED` publikovat jako retained stav,
- stav lokace `ONLINE/OFFLINE` publikovat jako retained stav,
- události publikovat bez retain,
- používat Last Will pro přechod lokace do `OFFLINE`,
- zpracování událostí navrhovat idempotentně,
- middleware musí tolerovat opakované doručení MQTT zprávy,
- chybějící nebo neplatný payload nesmí otevřít box.

Agent nesmí měnit topic namespace bez aktualizace dokumentace, konfigurace ESP a migračního plánu.

---

## 7. Databáze

- databáze je SQLite,
- přístup je přes Spring JDBC,
- SQL musí být explicitní a kontrolovatelné,
- cizí klíče a unikátní omezení mají být používány tam, kde chrání doménová pravidla,
- databázová operace přidělení boxu musí být atomická,
- nelze přidělit stejný box dvěma aktivním výdejům,
- změny obchodního stavu musí být auditovatelné.

MVP historicky používalo změny přímo v inicializačním `CREATE DB` skriptu.  
Pro již existující nasazení se však nesmí destruktivně měnit schéma bez výslovného migračního postupu.

Před změnou schématu agent musí určit, zda projekt pracuje pouze s čistě vytvářenou MVP databází, nebo s existujícími daty.

---

## 8. Bezpečnostní pravidla

Agent nesmí:

- vytvořit univerzální zákaznický PIN,
- zpřístupnit servisní PIN přes veřejné API bez autorizace,
- umožnit otevření boxu pouze podle čísla boxu bez ověření oprávnění,
- logovat celé PINy,
- vložit hesla, tokeny nebo MQTT přihlašovací údaje do Git repozitáře,
- při chybě komunikace automaticky předpokládat úspěšné otevření,
- deaktivovat validace za účelem „opravy“ testu.

Každý příkaz k otevření musí být dohledatelný alespoň podle:

- lokace,
- boxu,
- času,
- důvodu nebo typu oprávnění,
- výsledku,
- korelačního identifikátoru.

---

## 9. Testovací minimum

Každá změna relevantní doménové logiky musí mít test.

Minimálně ověřovat:

- produkt lze přidělit pouze z boxu, který aktuálně obsahuje příslušnou produktovou skupinu,
- změna obsahu boxu servisním doplněním změní dostupnost produktu,
- prázdný box, box s jiným produktem nebo box mimo provoz nelze přidělit danému produktu,
- dva aktivní výdeje nemohou použít stejný box,
- množství 2 vytvoří dvě různá přiřazení,
- čtyřmístný PIN není servisní PIN,
- šestimístný PIN není zákaznický PIN,
- neplatný PIN neotevře žádný box,
- opakovaná MQTT zpráva nezpůsobí nežádoucí druhou obchodní operaci,
- `OPEN`, `CLOSED`, `UNKNOWN` a případně `FAULT` se zpracují deterministicky,
- offline ESP nevede k falešnému potvrzení otevření.

Preferovat integrační testy databázových omezení před pouhým mockováním repository vrstvy.

---

## 10. Styl změn

- Provádět malé, tematicky související změny.
- Neměnit nesouvisející formátování.
- Nepřejmenovávat veřejné API bez důvodu.
- Neprovádět rozsáhlý refaktor současně s funkční opravou, není-li nezbytný.
- Nové chování nejprve zachytit testem.
- Komentáře používat pro důvod, nikoli pro opis kódu.
- Veřejné názvy psát konzistentně anglicky; projektová dokumentace může být česky.
- Chybové zprávy nesmí odhalovat PIN ani citlivé interní údaje.

---

## 11. Definice dokončení

Úloha není dokončena, dokud:

- odpovídá zadání,
- neporušuje doménová pravidla,
- změna je otestována,
- build nebo relevantní testy prošly,
- dokumentace je aktualizována, pokud se změnilo chování,
- nejsou skryté nevyřešené rozpory,
- shrnutí obsahuje změněné soubory, testy a známá rizika.
