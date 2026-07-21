# ADR-002: Dynamický obsah boxů podle servisního doplnění

- Status: Accepted

## Rozhodnutí

MVP používá tři produktové skupiny:

- A,
- B,
- C.

Produktová skupina není trvale svázaná s číslem fyzického boxu.

Při konstrukci výdejního místa nebo při servisním doplnění technik určí, který konkrétní box aktuálně obsahuje který produkt. Middleware musí tento stav evidovat a při objednávce přidělit jen box, který aktuálně obsahuje objednaný produkt a je obchodně dostupný.

Box může být také prázdný nebo mimo provoz. Takový box se nezapočítává do skladové dostupnosti žádného produktu.

## Důsledky

Výběr boxu se řídí aktuálním obsahem boxu na konkrétní lokaci, nikoli pevným mapováním čísla boxu na produkt.

Skladová dostupnost produktu se odvozuje z počtu boxů, které jsou na dané lokaci aktuálně naplněné tímto produktem, obchodně dostupné a nejsou rezervované, přidělené ani mimo provoz.

Objednávka quantity 2 vyžaduje dva různé boxy a dvě samostatná výdejní oprávnění.

Servisní změna obsahu boxu musí být auditovatelná, protože přímo mění skladovou dostupnost ve WooCommerce.
