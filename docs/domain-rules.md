# Doménová pravidla projektu

## Status dokumentu

Tento dokument popisuje aktuálně známý doménový model.  
Při rozporu má přednost `AGENTS.md` a přijatá rozhodnutí v `docs/decisions/`.

## 1. Základní pojmy

### Lokace

Jedno fyzické výdejní místo. Každá lokace má vlastní ESP a sadu boxů.

### Box

Fyzická uzamykatelná schránka. V jednom okamžiku obsahuje nejvýše jeden kus zboží.

### Produktová skupina

MVP obsahuje tři skupiny:

- A,
- B,
- C.

Produktová skupina není trvale svázaná s číslem boxu.

Konkrétní obsah boxu je provozní stav dané lokace. Při konstrukci výdejního místa nebo při servisním doplnění se určí, který box aktuálně obsahuje který produkt. Tuto informaci musí middleware evidovat a používat pro výpočet dostupnosti i pro přidělení objednávky.

Příklad stavu jedné lokace:

| Box | Obsah |
|---|---|
| 1 | A |
| 2 | C |
| 3 | A |
| 4 | B |
| 5 | prázdný |
| 6 | mimo provoz |

### Výdej

Konkrétní právo zákazníka vyzvednout jeden kus z konkrétního boxu pomocí konkrétního PINu.

### Zákaznický PIN

Čtyřmístný číselný PIN spojený s konkrétním výdejem.

### Servisní PIN

Šestimístný číselný PIN určený pro servisní operace. Je logicky i validačně oddělen od zákaznických PINů.

## 2. Životní cyklus boxu

Doporučené obchodní stavy:

- `AVAILABLE` — box je připraven k novému přidělení,
- `RESERVED` — box je atomicky rezervován pro vznikající objednávku nebo výdej,
- `ASSIGNED` — box je přidělen zákazníkovi,
- `EXPIRED` — lhůta výdeje uplynula a vyžaduje obsluhu,
- `OUT_OF_SERVICE` — box nelze přidělit,
- `UNKNOWN` — obchodní stav nelze bezpečně určit.

Fyzické stavy dveří jsou samostatná dimenze:

- `OPEN`
- `CLOSED`
- `UNKNOWN`
- volitelně `FAULT`

`CLOSED` neznamená `AVAILABLE`.  
`OPEN` neznamená, že byl výdej úspěšně dokončen.

## 3. Přidělování

Přidělení musí:

1. vybrat pouze box, který aktuálně obsahuje objednanou produktovou skupinu,
2. vybrat pouze box, který je obchodně dostupný,
3. proběhnout atomicky,
4. vytvořit auditovatelný záznam,
5. zabránit dvojímu aktivnímu přidělení.

Při objednaném množství `N` musí vzniknout `N` různých přidělení.  
Není dovoleno přidělit jeden box více kusům téže objednávky.

Pokud není dostatek boxů, operace nesmí vytvořit částečně platný stav bez výslovně navržené kompenzace.

## 4. Platnost výdeje

Výchozí lhůta je 3 dny.

Po expiraci:

- zákaznický výdej se nemá automaticky považovat za platný,
- zboží je určeno k osobnímu řešení na prodejně,
- stav musí zůstat auditovatelný,
- systém nesmí automaticky uvolnit box bez ověření, že je fyzicky vyprázdněn.

## 5. Otevření

Příkaz k otevření je povolen pouze po ověření:

- lokace,
- aktivního oprávnění,
- typu PINu,
- délky a číselného formátu PINu,
- platnosti výdeje nebo servisního oprávnění,
- cílového boxu.

Neplatný, expirovaný nebo nejednoznačný vstup znamená zamítnutí.

## 6. WooCommerce

WooCommerce je zdroj objednávky a platby. Middleware je zdroj pravdy pro konkrétní fyzické přidělení boxu.

Publikovaný sklad produktu má odpovídat počtu boxů, které jsou na dané lokaci aktuálně naplněné daným produktem, obchodně dostupné a nejsou rezervované, přidělené ani mimo provoz.

Synchronizace musí počítat s:

- opakováním webhooku,
- pozdním doručením,
- stornem objednávky,
- nedokončenou platbou,
- množstvím vyšším než 1,
- krátkodobým souběhem objednávek.

## 7. Audit

Auditovat minimálně:

- vznik a zánik rezervace,
- přidělení boxu,
- vytvoření a expiraci výdeje,
- pokus o otevření,
- výsledek hardwarového příkazu,
- změny stavu dveří,
- servisní zásah,
- ruční zásah obsluhy.

Audit nesmí obsahovat celý PIN.
