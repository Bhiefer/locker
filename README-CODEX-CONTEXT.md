# Kontext pro Codex

Tato sada souborů převádí dosavadní projektové diskuse o `cz.evans.locker` do strukturované dokumentace.

## Doporučené umístění

Zkopírujte obsah do kořene repozitáře:

```text
AGENTS.md
README-CODEX-CONTEXT.md
docs/
```

## První pokyn pro Codex

```text
Přečti AGENTS.md a všechny dokumenty v docs/.
Potom projdi aktuální repozitář a testy.
Nic zatím neupravuj.

Vytvoř:
1. přehled skutečné architektury,
2. seznam rozporů mezi kódem a dokumentací,
3. seznam chybějících testů,
4. návrh nejmenšího bezpečného plánu nápravy.

Každé tvrzení podlož konkrétním souborem nebo třídou.
Neodhaduj otevřené otázky z docs/open-questions.md.
```

## Důležité

Dokumentace vychází z dosavadních diskusí, nikoli z automatické kontroly aktuálního repozitáře. Proto musí Codex při prvním běhu provést rozdílovou analýzu.

`AGENTS.md` obsahuje závazná pravidla.  
`docs/open-questions.md` obsahuje věci, které zatím nesmí agent svévolně rozhodnout.
