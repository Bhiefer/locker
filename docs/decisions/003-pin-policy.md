# ADR-003: Politika PINů

- Status: Accepted

## Rozhodnutí

- zákaznický PIN: přesně 4 číslice,
- servisní PIN: přesně 6 číslic,
- oba typy musí být validačně oddělené,
- v současném MVP je zákaznický PIN uložen čitelně,
- celé PINy se nesmí zapisovat do běžných logů.

## Poznámka

Čitelné ukládání je vědomé MVP rozhodnutí. Není dovoleno jej tiše měnit bez dopadu na provoz, migraci a obsluhu zařízení.
