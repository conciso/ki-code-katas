# Projekt-Richtlinien

## Test-Driven Development (TDD)

Wir arbeiten strikt testgetrieben nach dem Red-Green-Refactor Zyklus:

1. **Red**: Zuerst einen Test schreiben, der fehlschlägt
2. **Review**: Den Test gemeinsam anschauen und ggf. verbessern, bevor die Implementierung beginnt
3. **Green**: Dann die minimale Implementierung schreiben, die den Test erfüllt
4. **Refactor**: Code verbessern, während alle Tests grün bleiben

### Workflow

- Nie Produktionscode ohne vorherigen fehlschlagenden Test schreiben
- Tests müssen erst rot sein, bevor sie grün werden dürfen
- Nach dem Schreiben eines Tests: Pause für Review mit dem User
- **WICHTIG**: Nach dem Review auf EXPLIZITE Freigabe warten (z.B. "implementiere", "mach grün", "schreib die Funktion")
- "Test ausführen" oder "Test passt" bedeutet NICHT "implementiere" - nur den fehlschlagenden Test zeigen
- Nie Red und Green in einem Schritt kombinieren
