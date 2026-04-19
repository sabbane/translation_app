# Projekt Spezifikation: Übersetzungs- und Review-Tool

## 1. Technologie-Stack
* **Backend:** Java, Spring Boot (REST API)
* **Frontend:** React, TypeScript (empfohlen für bessere Typsicherheit)
* **Datenbank:** PostgreSQL
* **Authentifizierung:** Spring Security mit JWT (JSON Web Tokens)

## 2. Kernfunktionalität & UI
* **Split-Screen-Editor:** Die Benutzeroberfläche stellt ein zweigeteiltes Textfeld bereit. Auf der linken Seite wird der Originaltext eingegeben/angezeigt, auf der rechten Seite die Übersetzung.
* **Unterstützte Sprachen:** Englisch, Deutsch, Französisch (sowohl als Quell- als auch als Zielsprache wählbar).

## 3. Rollen und Berechtigungen (RBAC)
Das System nutzt Role-Based Access Control (RBAC) mit drei spezifischen Rollen:

| Rolle | Zugriffsrechte |
| :--- | :--- |
| **Admin** | Vollzugriff auf alle Dokumente. Exklusiver Zugriff auf die Benutzerverwaltung (CRUD für User). |
| **User** | Darf neue Übersetzungsdokumente erstellen und den Text übersetzen. Darf das Dokument einem spezifischen Reviewer zuweisen. Kein Zugriff auf die Benutzerverwaltung. |
| **Reviewer** | Nur Lese- und Schreibzugriff auf Dokumente, die ihm explizit zugewiesen wurden. Darf diese editieren und den Status ändern. |

## 4. Dokumenten-Workflow und Status
Jedes Übersetzungsdokument durchläuft einen definierten Lebenszyklus mit folgenden Statuswerten:

| Status | Beschreibung |
| :--- | :--- |
| **Offen** | Ein neues Dokument wurde erstellt, aber die Übersetzung ist noch nicht abgeschlossen. |
| **Übersetzt** | Der "User" hat die Übersetzung beendet und das Dokument einem "Reviewer" zugewiesen. |
| **Bestätigt** | Der zugewiesene "Reviewer" hat die Übersetzung geprüft, ggf. editiert und final abgenommen. |

## 5. Externe API-Integration (Automatische Vorübersetzung)
* **Tool:** DeepL API Free (REST API).
* **Backend-Architektur:** Es wird ein eigener Spring Boot Service benötigt, der die REST-Anfragen an die DeepL API sendet. Der API-Key muss über die `application.properties` bzw. als Umgebungsvariable eingebunden werden.
* **Frontend UI-Interaktion:** * Die Vorübersetzung wird durch einen expliziten Button (z. B. "Automatisch Übersetzen") im Editor ausgelöst.
  * Beim Klick wird der Button blockiert (Ladezustand), um Mehrfachklicks zu vermeiden.
  * Das Backend kommuniziert mit DeepL und sendet das Ergebnis zurück an das Frontend, welches die rechte Seite (Übersetzung) füllt.
  * Status: Eine maschinell erstellte Übersetzung ändert den Dokumentenstatus *nicht*. Der Status bleibt "Offen", bis der User manuell bestätigt und weiterleitet.

## 6. Reviewer-Zuweisung (Workflow)
* **UI-Element:** Ein Dropdown-Menü im Editor-Bereich, sichtbar für "Users".
* **Datenquelle:** Dynamisch befüllt mit allen Benutzern der Rolle `REVIEWER` via Backend-Endpunkt (z. B. `GET /api/users/reviewers`).
* **Ablauf:** Der "User" wählt den Reviewer, klickt auf "Zur Überprüfung einreichen". Das Backend verknüpft die `reviewer_id` und ändert den Status auf "Übersetzt". Danach ist das Dokument für den "User" schreibgeschützt (Read-Only).

## 7. Löschberechtigungen für Dokumente
* **Admin:** Kann jedes Dokument im System jederzeit löschen (unabhängig vom Status).
* **User:** Darf ausschließlich Dokumente löschen, die er selbst erstellt hat (strikte Prüfung der `creator_id` im Backend).
* **Reviewer:** Keine Berechtigung zum Löschen.
* **Frontend:** Ein "Löschen"-Button wird nur gerendert, wenn der User die entsprechende Berechtigung für dieses spezifische Dokument besitzt.

## 8. Dashboards und Listenansichten
Nach dem Login sehen Benutzer ein rollenspezifisches Dashboard mit einer Datentabelle.

* **Gemeinsame Spalten:** Auszug Originaltext (50 Zeichen), Quell-/Zielsprache, Status (farbige Badges), Aktions-Buttons.
* **Admin Dashboard:** Zeigt alle Dokumente. Zusätzliche Spalten für Ersteller und Reviewer. Globale Filter/Suche. Separater Bereich für die Benutzerverwaltung.
* **User Dashboard:** Zeigt nur Dokumente mit eigener `creator_id`. Prominenter "Neues Dokument"-Button.
* **Reviewer Dashboard:** Zeigt nur Dokumente mit eigener `reviewer_id`. Fokus liegt standardmäßig auf Dokumenten mit dem Status "Übersetzt".

## 9. Paginierung und Sortierung (Performance)
* **Serverseitige Paginierung:** Das Backend liefert via `Pageable` nur die Datensätze einer Seite (Standard: 20 Dokumente pro Seite). Frontend zeigt Paginierungs-Controls.
* **Sortierung:** Standardmäßig absteigend nach Erstelldatum (`created_at`). Spaltenköpfe im Frontend sind für dynamische Sortierung anklickbar.

## 10. Vorläufiges Datenmodell (Fokus-Entitäten)

### User
* `id` (UUID/Long)
* `username` (String, unique)
* `password_hash` (String)
* `role` (Enum: ADMIN, USER, REVIEWER)

### Document
* `id` (UUID/Long)
* `original_text` (Text)
* `translated_text` (Text)
* `source_language` (String/Enum)
* `target_language` (String/Enum)
* `status` (Enum: OFFEN, UEBERSETZT, BESTAETIGT)
* `is_auto_translated` (Boolean)
* `created_at` (Timestamp)
* `creator_id` (Foreign Key -> User)
* `reviewer_id` (Foreign Key -> User, nullable)

## 11. Erste Schritte für den Code-Assistenten
1. Initialisierung des Spring Boot Projekts mit den nötigen Dependencies (Web, Data JPA, PostgreSQL Driver, Security).
2. Erstellung der Entity-Klassen (`User`, `Document`) und der entsprechenden Spring Data JPA Repositories.
3. Aufbau der REST-Controller für CRUD-Operationen unter Berücksichtigung der RBAC, Paginierung und Lösch-Validierung.