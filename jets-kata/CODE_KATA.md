# Code Kata — Jets Multiplayer

## Konzept

In dieser Code Kata entwickeln Zweierteams ein Multiplayer-Arcade-Spiel. Jedes Team implementiert entweder das Frontend oder das Backend des Jets-Spiels und wählt dabei frei zwischen zwei Technologie-Stacks. Alle Teams arbeiten gegen dasselbe WebSocket-Protokoll — am Ende werden Frontend- und Backend-Teams zusammengeschaltet.

## Zeitrahmen

**4 Stunden** (inkl. Pausen)

| Phase | Dauer | Inhalt |
|-------|-------|--------|
| Intro | 15 min | Spielvorstellung, Regeln, Teambildung |
| Setup | 15 min | Projekt klonen, Stack wählen, Umgebung prüfen |
| Iteration 1 | 60 min | Lobby-System (WebSocket-Verbindung, Create/Join) |
| Pause | 10 min | |
| Iteration 2 | 60 min | Spiellogik (Game Loop, Input, State) |
| Pause | 10 min | |
| Iteration 3 | 45 min | Integration & Polish |
| Showcase | 15 min | Teams schalten ihre Implementierungen zusammen |

## Teams & Auswahl

Jedes Zweierteam wählt **eine Seite** und **einen Stack**:

| Seite | Stack A | Stack B |
|-------|---------|---------|
| **Frontend** | Angular 21 | Vue 3.5 |
| **Backend** | Spring Boot 4 (Java) | .NET 10 (C#) |

Die Projektgerüste für alle vier Kombinationen sind vorbereitet. Teams müssen nur klonen und loslegen.

## Regeln

### Test-Driven Development

1. **Red** — Schreibe einen fehlschlagenden Test für das nächste Verhalten
2. **Green** — Schreibe den minimalen Code, damit der Test besteht
3. **Refactor** — Verbessere den Code, alle Tests bleiben grün

Jede neue Funktionalität beginnt mit einem Test. Code ohne zugehörigen Test zählt nicht.

### KI-Nutzung

KI-Assistenten (Copilot, ChatGPT, Claude, etc.) dürfen genutzt werden — mit Einschränkungen:

- **Erlaubt:** Einzelne, gezielte Fragen an die KI stellen ("Wie konfiguriere ich einen WebSocket-Endpoint in Spring Boot?", "Schreibe einen Unit-Test für die Kollisionserkennung")
- **Erlaubt:** Code-Vervollständigung und Vorschläge für einzelne Methoden oder Tests
- **Nicht erlaubt:** Ganze Features oder Module am Stück von der KI generieren lassen
- **Nicht erlaubt:** Das gesamte Protokoll oder die Spielbeschreibung in die KI kopieren und "Implementiere das" sagen

**Faustregel:** Die KI ist ein Werkzeug für Einzelschritte, nicht der Entwickler. Beide Teammitglieder müssen jede Zeile Code verstehen und erklären können.

### Pair Programming

Beide Teammitglieder arbeiten gemeinsam an einem Rechner. Empfohlen wird das Driver/Navigator-Modell mit regelmäßigem Wechsel (alle 15–20 Minuten).

### Protokolltreue

Alle Teams implementieren gegen das dokumentierte WebSocket-Protokoll (`WEBSOCKET_PROTOCOL.md`). Das Protokoll ist der Vertrag — wer sich daran hält, kann am Ende jedes Frontend mit jedem Backend kombinieren.

## Iterationen

### Iteration 1 — Lobby

**Ziel:** Spieler können eine Lobby erstellen, beitreten und sich bereit melden.

Backend-Teams:
- WebSocket-Endpoint + Verbindungsverwaltung
- Lobby erstellen/beitreten (6-stelliger Code)
- Lobby-State an alle Clients broadcasten
- Validierung (max. 4 Spieler, Lobby existiert, etc.)

Frontend-Teams:
- WebSocket-Verbindung aufbauen
- Lobby erstellen / Code eingeben und beitreten
- Spielerliste anzeigen, Ready-Button
- Fehlerbehandlung (Lobby voll, nicht gefunden)

### Iteration 2 — Spiellogik

**Ziel:** Jets fliegen, schießen und interagieren.

Backend-Teams:
- Server-seitige Game Loop (30 Ticks/Sekunde)
- Input-Verarbeitung, Positionsberechnung
- Projektil-Verwaltung und Kollisionserkennung
- Game State an alle Clients senden

Frontend-Teams:
- Canvas-Rendering (Jets, Projektile, Gegner)
- Tastatur-Input erfassen und an Server senden
- Game State empfangen und darstellen
- HUD (Score, HP, Welle)

### Iteration 3 — Integration & Polish

**Ziel:** Frontend und Backend zusammenschalten und spielbar machen.

- Cross-Team-Tests: Frontend-Team A testet mit Backend-Team B
- Bugs fixen, Timing-Probleme beheben
- Optionale Features: Power-Ups, Gegnertypen, Respawn, Effekte

## Bewertungskriterien

Es gibt keine formale Bewertung — aber am Showcase achten wir auf:

| Kriterium | Frage |
|-----------|-------|
| **Funktionalität** | Läuft das Spiel? Können Spieler joinen und spielen? |
| **Testabdeckung** | Sind die Kernmechaniken durch Tests abgesichert? |
| **Protokolltreue** | Funktioniert das Zusammenschalten mit anderen Teams? |
| **Code-Qualität** | Ist der Code lesbar, strukturiert, wartbar? |
| **Teamwork** | Haben beide Teammitglieder aktiv beigetragen? |

## Technische Voraussetzungen

Auf jedem Rechner muss installiert sein:
- Node.js 24+
- Java 21+ mit Maven
- .NET 10 SDK
- Ein Browser mit DevTools
- Eine IDE nach Wahl

## Referenzdokumente

- `GAME_DESCRIPTION.md` — Spielmechaniken, Gegnertypen, Power-Ups
- `WEBSOCKET_PROTOCOL.md` — Vollständiges Nachrichtenprotokoll mit Beispielen
