# Jets — Multiplayer Arcade Shooter

## Überblick

Jets ist ein Multiplayer-Arcade-Shooter für bis zu 4 Spieler. Die Spieler steuern Kampfjets in einer 2D-Arena und bekämpfen gemeinsam im Co-Op-Modus ankommende Gegnerwellen oder treten im PvP-Modus gegeneinander an.

## Spielmodi

### Co-Op (2–4 Spieler)

Alle Spieler kämpfen gemeinsam gegen computergesteuerte Gegnerwellen. Die Wellen werden mit jeder Runde schwieriger — mehr Gegner, schnellere Bewegungen, neue Gegnertypen. Das Spiel endet, wenn alle Spieler eliminiert sind. Ziel ist es, gemeinsam den höchstmöglichen Score zu erreichen.

### Free-for-All (2–4 Spieler)

Jeder Spieler kämpft gegen jeden. Treffer auf andere Spieler geben Punkte, Abschüsse geben Bonuspunkte. Das Spiel endet nach Ablauf eines Zeitlimits oder wenn ein Spieler eine festgelegte Punktzahl erreicht.

## Spielmechaniken

### Steuerung

| Taste       | Aktion           |
|-------------|------------------|
| W / ↑       | Nach oben        |
| S / ↓       | Nach unten       |
| A / ←       | Nach links       |
| D / →       | Nach rechts      |
| Leertaste   | Schießen         |

### Spieler-Jet

- **Hitpoints:** 3
- **Geschwindigkeit:** Konstant, diagonal leicht reduziert (Normalisierung)
- **Feuerrate:** Max. 5 Schüsse pro Sekunde (Cooldown 200ms)
- **Projektilgeschwindigkeit:** 2× Jet-Geschwindigkeit
- **Respawn:** Nach Zerstörung 3 Sekunden Wartezeit, 2 Sekunden Unverwundbarkeit nach Respawn
- **Leben:** 3 Respawns im Co-Op, unbegrenzt im FFA

### Gegnertypen (Co-Op)

| Typ       | HP | Geschwindigkeit | Verhalten                          | Punkte |
|-----------|----|-----------------|-------------------------------------|--------|
| Scout     | 1  | Schnell         | Fliegt geradeaus                   | 100    |
| Fighter   | 2  | Mittel          | Verfolgt nächsten Spieler          | 250    |
| Bomber    | 4  | Langsam         | Fliegt geradeaus, schießt nach unten | 500   |
| Boss      | 20 | Sehr langsam    | Erscheint alle 5 Wellen, schießt gezielt | 2000 |

### Wellen (Co-Op)

- **Welle 1–3:** Nur Scouts
- **Welle 4–6:** Scouts + Fighter
- **Welle 7–9:** Scouts + Fighter + Bomber
- **Welle 10:** Boss + gemischte Gegner
- Ab Welle 11 wiederholt sich das Muster mit steigender Gegneranzahl und Geschwindigkeit (+10% pro Zyklus)

### Power-Ups

Power-Ups erscheinen zufällig nach dem Zerstören von Gegnern (20% Chance).

| Power-Up     | Effekt                              | Dauer     |
|--------------|--------------------------------------|-----------|
| Rapid Fire   | Feuerrate verdoppelt                | 8 Sekunden |
| Shield       | Absorbiert den nächsten Treffer     | Einmalig   |
| Speed Boost  | Geschwindigkeit +50%                | 6 Sekunden |
| Health Pack  | +1 HP (max. 3)                      | Sofort     |

## Spielfeld

- **Größe:** 1920 × 1080 logische Einheiten
- **Skalierung:** Das Spielfeld wird proportional auf die Bildschirmgröße skaliert
- **Ränder:** Spieler-Jets werden an den Rändern gestoppt, Projektile und Gegner werden beim Verlassen des Spielfelds entfernt

## Lobby-System

1. Ein Spieler erstellt eine Lobby und erhält einen 6-stelligen Code (z.B. `A3F9K2`)
2. Andere Spieler treten mit dem Code bei
3. Der Ersteller (Host) wählt den Spielmodus und startet das Spiel
4. Mindestens 2 Spieler sind erforderlich

## Punktevergabe

### Co-Op
- Punkte werden pro Spieler gezählt
- Am Ende wird ein Gesamtscore aus allen Spielern berechnet
- Leaderboard speichert den Gesamtscore mit den Namen aller Spieler

### Free-for-All
- +100 Punkte pro Treffer auf einen anderen Spieler
- +500 Punkte pro Abschuss
- -200 Punkte bei eigenem Tod
- Leaderboard zeigt Einzelspieler-Scores

## Technische Rahmenbedingungen

- **Server-Tickrate:** 30 Ticks pro Sekunde
- **Client-Framerate:** 60 FPS (requestAnimationFrame)
- **Netzwerk:** WebSocket-Verbindung, Server ist autoritativ
- **Client-Side Prediction:** Eigener Jet wird lokal sofort bewegt, Server korrigiert bei Abweichung
- **Interpolation:** Andere Spieler und Gegner werden zwischen Server-Updates interpoliert
