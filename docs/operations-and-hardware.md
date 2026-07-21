# Provoz, síť a hardware

## 1. Fyzická konfigurace

- aktuální MVP: 6 boxů,
- budoucí lokace mohou mít přibližně 5–10 boxů,
- jedno ESP na jednu lokaci,
- elektrické zámky se senzory dveří,
- klávesnice pro zadání PINu,
- volitelný jednoduchý displej,
- bez složitého lokálního UI.

## 2. Napájení

Plánovaná autonomní sestava:

- 3 × 100 W solární panel,
- LiFePO4 baterie 12,8 V,
- LTE router,
- ESP a zámky.

Software musí počítat s:

- náhlým výpadkem,
- opakovaným připojením,
- obnovou retained MQTT stavů,
- neúplným příkazem,
- resetem ESP během otevírání.

## 3. Konektivita

Počáteční provoz může být v lokální síti.

Budoucí více lokalit:

- preferováno privátní propojení přes Tailscale nebo VPN,
- nepředpokládat stabilní veřejnou IP,
- nepředpokládat trvale dostupné LTE,
- síťový výpadek nesmí vést k neoprávněnému otevření.

## 4. Autorita stavů

ESP:

- měří fyzický stav dveří,
- provádí hardwarový příkaz,
- hlásí diagnostiku.

Middleware:

- rozhoduje o oprávnění,
- spravuje výdeje a obchodní stav,
- vytváří audit,
- sleduje stav příkazu.

WooCommerce:

- eviduje objednávku a platbu,
- není autoritou pro skutečný stav konkrétního boxu.

Home Assistant:

- může zobrazovat odvozené stavy,
- nemá být primární autoritou obchodní logiky.

## 5. Bezpečný režim

Při nejasném stavu se systém musí přiklonit k bezpečnému zamítnutí.

Příklady:

- middleware nedostupný → běžný zákaznický PIN neotevírat, pokud není výslovně navržen bezpečný offline režim,
- ESP offline → nepotvrzovat úspěšné otevření,
- neznámý stav dveří → neuvolnit box pouze na základě času,
- duplicitní MQTT příkaz → neopakovat fyzickou akci bez definované idempotence.

## 6. Servis

Servisní operace musí být odděleně auditovány.

Šestimístný servisní PIN je současný požadavek, ale přesný rozsah jeho oprávnění musí být v implementaci explicitní. Agent nesmí automaticky předpokládat, že servisní PIN smí otevřít všechny boxy, pokud to není potvrzeno aktuálním zadáním nebo testy.
