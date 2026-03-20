# WebSocket-Protokoll — Jets Multiplayer

## Verbindung

```
ws://{host}:{port}/ws/game?playerName={name}
```

Nach dem Verbindungsaufbau erhält der Client eine `CONNECTED`-Nachricht mit seiner Player-ID.

## Nachrichtenformat

Alle Nachrichten werden als JSON übertragen. Jede Nachricht hat ein `type`-Feld, das den Nachrichtentyp identifiziert.

```json
{
  "type": "MESSAGE_TYPE",
  "data": { ... }
}
```

---

## Verbindungsnachrichten

### CONNECTED (Server → Client)

Bestätigung nach erfolgreichem Verbindungsaufbau.

```json
{
  "type": "CONNECTED",
  "data": {
    "playerId": "p1a2b3c4",
    "serverTickRate": 30
  }
}
```

### DISCONNECTED (Server → alle Clients)

Ein Spieler hat die Verbindung verloren.

```json
{
  "type": "DISCONNECTED",
  "data": {
    "playerId": "p1a2b3c4",
    "playerName": "Alice"
  }
}
```

### PING / PONG (Client ↔ Server)

Heartbeat zur Latenz-Messung. Der Client sendet `PING`, der Server antwortet mit `PONG`.

```json
{ "type": "PING", "data": { "timestamp": 1710950400000 } }
{ "type": "PONG", "data": { "timestamp": 1710950400000 } }
```

### ERROR (Server → Client)

Fehlermeldung bei ungültigen Aktionen.

```json
{
  "type": "ERROR",
  "data": {
    "code": "LOBBY_FULL",
    "message": "Die Lobby ist voll (max. 4 Spieler)"
  }
}
```

Fehlercodes:

| Code               | Bedeutung                          |
|--------------------|------------------------------------|
| `LOBBY_FULL`       | Lobby hat bereits 4 Spieler       |
| `LOBBY_NOT_FOUND`  | Lobby-Code existiert nicht        |
| `GAME_IN_PROGRESS` | Spiel hat bereits begonnen        |
| `NOT_HOST`         | Nur der Host darf diese Aktion ausführen |
| `INVALID_MESSAGE`  | Nachricht konnte nicht verarbeitet werden |

---

## Lobby-Nachrichten

### CREATE_LOBBY (Client → Server)

Erstellt eine neue Lobby. Der Ersteller wird automatisch Host.

```json
{
  "type": "CREATE_LOBBY",
  "data": {
    "playerName": "Alice"
  }
}
```

### LOBBY_CREATED (Server → Client)

Bestätigung der Lobby-Erstellung.

```json
{
  "type": "LOBBY_CREATED",
  "data": {
    "lobbyCode": "A3F9K2",
    "hostId": "p1a2b3c4"
  }
}
```

### JOIN_LOBBY (Client → Server)

Einer bestehenden Lobby beitreten.

```json
{
  "type": "JOIN_LOBBY",
  "data": {
    "lobbyCode": "A3F9K2",
    "playerName": "Bob"
  }
}
```

### LOBBY_STATE (Server → alle Clients in Lobby)

Wird nach jeder Änderung an der Lobby gesendet (Beitritt, Verlassen, Moduswechsel).

```json
{
  "type": "LOBBY_STATE",
  "data": {
    "lobbyCode": "A3F9K2",
    "hostId": "p1a2b3c4",
    "gameMode": "COOP",
    "players": [
      { "id": "p1a2b3c4", "name": "Alice", "ready": true, "color": "#FF4444" },
      { "id": "p2d5e6f7", "name": "Bob", "ready": false, "color": "#4444FF" }
    ]
  }
}
```

### SET_GAME_MODE (Client → Server)

Nur der Host kann den Spielmodus ändern.

```json
{
  "type": "SET_GAME_MODE",
  "data": {
    "gameMode": "COOP"
  }
}
```

Gültige Werte: `COOP`, `FFA`

### PLAYER_READY (Client → Server)

Spieler signalisiert Bereitschaft.

```json
{
  "type": "PLAYER_READY",
  "data": {
    "ready": true
  }
}
```

### START_GAME (Client → Server)

Nur der Host kann das Spiel starten. Voraussetzung: mindestens 2 Spieler, alle bereit.

```json
{
  "type": "START_GAME",
  "data": {}
}
```

### LEAVE_LOBBY (Client → Server)

Spieler verlässt die Lobby. Wenn der Host verlässt, wird der nächste Spieler zum Host.

```json
{
  "type": "LEAVE_LOBBY",
  "data": {}
}
```

---

## Spielnachrichten

### GAME_STARTING (Server → alle Clients)

Countdown vor Spielbeginn.

```json
{
  "type": "GAME_STARTING",
  "data": {
    "countdown": 3,
    "gameMode": "COOP",
    "fieldWidth": 1920,
    "fieldHeight": 1080,
    "players": [
      { "id": "p1a2b3c4", "name": "Alice", "color": "#FF4444", "spawnX": 200, "spawnY": 540 },
      { "id": "p2d5e6f7", "name": "Bob", "color": "#4444FF", "spawnX": 600, "spawnY": 540 }
    ]
  }
}
```

### PLAYER_INPUT (Client → Server)

Der Client sendet den aktuellen Input-Zustand. Wird bei jeder Änderung gesendet (Taste gedrückt/losgelassen), nicht jeden Frame.

```json
{
  "type": "PLAYER_INPUT",
  "data": {
    "up": false,
    "down": false,
    "left": true,
    "right": false,
    "shoot": true,
    "seq": 142
  }
}
```

`seq` ist eine aufsteigende Sequenznummer für Client-Side Prediction. Der Server bestätigt die letzte verarbeitete Sequenznummer im Game State.

### GAME_STATE (Server → alle Clients)

Vollständiger Spielzustand, gesendet mit der Server-Tickrate (30×/Sekunde).

```json
{
  "type": "GAME_STATE",
  "data": {
    "tick": 1847,
    "players": [
      {
        "id": "p1a2b3c4",
        "x": 450.5,
        "y": 320.0,
        "hp": 3,
        "score": 1200,
        "alive": true,
        "respawnIn": 0,
        "invincible": false,
        "activePowerUp": null,
        "lastProcessedInput": 142
      },
      {
        "id": "p2d5e6f7",
        "x": 800.0,
        "y": 600.0,
        "hp": 1,
        "score": 800,
        "alive": true,
        "respawnIn": 0,
        "invincible": false,
        "activePowerUp": "RAPID_FIRE",
        "lastProcessedInput": 98
      }
    ],
    "projectiles": [
      { "id": "b001", "x": 500.0, "y": 300.0, "vx": 0.0, "vy": -10.0, "owner": "p1a2b3c4" },
      { "id": "b002", "x": 810.0, "y": 580.0, "vx": 5.0, "vy": -8.0, "owner": "p2d5e6f7" }
    ],
    "enemies": [
      { "id": "e001", "type": "SCOUT", "x": 900.0, "y": 50.0, "hp": 1 },
      { "id": "e002", "type": "FIGHTER", "x": 400.0, "y": 100.0, "hp": 2 }
    ],
    "powerUps": [
      { "id": "pu001", "type": "SHIELD", "x": 700.0, "y": 400.0 }
    ],
    "wave": 3,
    "enemiesRemaining": 8
  }
}
```

### GAME_EVENT (Server → alle Clients)

Einmalige Ereignisse für visuelle/akustische Effekte. Diese Daten sind nicht im `GAME_STATE` enthalten, da sie nur einmal auftreten.

```json
{
  "type": "GAME_EVENT",
  "data": {
    "event": "EXPLOSION",
    "x": 900.0,
    "y": 50.0,
    "details": {
      "destroyedType": "SCOUT",
      "destroyedBy": "p1a2b3c4"
    }
  }
}
```

Event-Typen:

| Event            | Beschreibung                           | Details                              |
|------------------|----------------------------------------|--------------------------------------|
| `EXPLOSION`      | Gegner oder Spieler zerstört          | `destroyedType`, `destroyedBy`       |
| `PLAYER_HIT`     | Spieler wurde getroffen               | `playerId`, `damage`, `hitBy`        |
| `PLAYER_KILLED`  | Spieler hat alle HP verloren          | `playerId`, `killedBy`               |
| `PLAYER_RESPAWN` | Spieler ist zurückgekehrt             | `playerId`, `x`, `y`                |
| `POWERUP_SPAWN`  | Neues Power-Up erschienen             | `powerUpId`, `type`, `x`, `y`       |
| `POWERUP_PICKUP` | Spieler hat Power-Up eingesammelt     | `playerId`, `type`                   |
| `WAVE_START`     | Neue Welle beginnt                    | `wave`, `enemyCount`                |
| `BOSS_SPAWN`     | Boss-Gegner erscheint                | `enemyId`, `x`, `y`                 |

### WAVE_COMPLETE (Server → alle Clients)

Eine Welle wurde abgeschlossen (Co-Op).

```json
{
  "type": "WAVE_COMPLETE",
  "data": {
    "wave": 3,
    "nextWaveIn": 5,
    "scores": {
      "p1a2b3c4": 1200,
      "p2d5e6f7": 800
    }
  }
}
```

### GAME_OVER (Server → alle Clients)

Das Spiel ist beendet.

```json
{
  "type": "GAME_OVER",
  "data": {
    "reason": "ALL_DEAD",
    "finalScores": [
      { "playerId": "p1a2b3c4", "name": "Alice", "score": 4500, "kills": 32 },
      { "playerId": "p2d5e6f7", "name": "Bob", "score": 3200, "kills": 21 }
    ],
    "totalScore": 7700,
    "wavesCompleted": 7
  }
}
```

Gründe (`reason`):

| Reason           | Modus | Beschreibung                         |
|------------------|-------|--------------------------------------|
| `ALL_DEAD`       | Co-Op | Alle Spieler haben keine Leben mehr |
| `TIME_UP`        | FFA   | Zeitlimit erreicht                  |
| `SCORE_REACHED`  | FFA   | Ein Spieler hat das Punktelimit erreicht |
| `ALL_LEFT`       | Beide | Alle Spieler bis auf einen haben die Verbindung verloren |

### RETURN_TO_LOBBY (Client → Server)

Nur der Host. Bringt alle Spieler zurück in die Lobby.

```json
{
  "type": "RETURN_TO_LOBBY",
  "data": {}
}
```

---

## Nachrichtenfluss

### Lobby → Spiel

```
Client A            Server              Client B
   |                  |                    |
   |-- CREATE_LOBBY ->|                    |
   |<- LOBBY_CREATED -|                    |
   |<- LOBBY_STATE ---|                    |
   |                  |<-- JOIN_LOBBY -----|
   |<- LOBBY_STATE ---|-- LOBBY_STATE ---->|
   |                  |                    |
   |-- PLAYER_READY ->|                    |
   |<- LOBBY_STATE ---|-- LOBBY_STATE ---->|
   |                  |<-- PLAYER_READY ---|
   |<- LOBBY_STATE ---|-- LOBBY_STATE ---->|
   |                  |                    |
   |-- START_GAME --->|                    |
   |<- GAME_STARTING -|-- GAME_STARTING ->|
   |    (countdown)   |    (countdown)     |
   |                  |                    |
```

### Spielschleife

```
Client A            Server              Client B
   |                  |                    |
   |-- PLAYER_INPUT ->|                    |
   |                  |<-- PLAYER_INPUT ---|
   |                  |                    |
   |              [Server Tick]            |
   |              - Inputs verarbeiten     |
   |              - Positionen updaten     |
   |              - Kollisionen prüfen     |
   |              - Gegner spawnen/bewegen |
   |                  |                    |
   |<-- GAME_STATE ---|--- GAME_STATE ---->|
   |<-- GAME_EVENT ---|--- GAME_EVENT ---->|  (bei Ereignissen)
   |                  |                    |
   |         ... nächster Tick ...         |
   |                  |                    |
```

### Spielende → Lobby

```
Client A            Server              Client B
   |                  |                    |
   |<--- GAME_OVER ---|---- GAME_OVER --->|
   |                  |                    |
   | - RETURN_TO_LOBBY|                    |
   |<- LOBBY_STATE ---|-- LOBBY_STATE ---->|
   |                  |                    |
```

---

## Bandbreiten-Optimierung

### Delta-Kompression (optional)

Wenn die Bandbreite knapp wird, kann der Server statt des vollständigen `GAME_STATE` nur Änderungen seit dem letzten Tick senden:

```json
{
  "type": "GAME_STATE_DELTA",
  "data": {
    "tick": 1848,
    "baseTick": 1847,
    "players": {
      "p1a2b3c4": { "x": 455.0, "y": 318.0, "lastProcessedInput": 143 }
    },
    "removedProjectiles": ["b001"],
    "removedEnemies": ["e001"]
  }
}
```

Der Client muss fehlende Deltas erkennen (Tick-Lücke) und einen vollständigen State anfordern:

```json
{ "type": "REQUEST_FULL_STATE", "data": {} }
```

### Nachrichtengrößen (geschätzt)

| Nachricht      | Größe        | Frequenz          |
|----------------|-------------|-------------------|
| `PLAYER_INPUT` | ~80 Bytes   | Bei Änderung      |
| `GAME_STATE`   | ~500–1500 Bytes | 30×/Sekunde    |
| `GAME_EVENT`   | ~150 Bytes  | Bei Ereignis      |
| `LOBBY_STATE`  | ~300 Bytes  | Bei Änderung      |
