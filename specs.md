# Projekt Spezifikation: Übersetzungs- und Review-Tool

## 1. Technologie-Stack
* **Backend:** Java, Spring Boot (REST API)
* **Frontend:** React, TypeScript (Vite)
* **Datenbank:** PostgreSQL (Lokal: `translation_db`, Test: `translation_db_test`)
* **Authentifizierung:** Spring Security mit JWT (JSON Web Tokens)
* **Testing:** JUnit 5, Mockito, TestRestTemplate, Fake Repositories (Unit & Integration Tests)

## 2. Kernfunktionalität & UI
* **Split-Screen-Editor:** Die Benutzeroberfläche stellt ein zweigeteiltes Textfeld bereit. Auf der linken Seite wird der Originaltext eingegeben/angezeigt, auf der rechten Seite die Übersetzung.
* **Modernes Design:** Verwendung von Lucide-Icons (Pencil, Trash2, UserPlus) anstelle von Text-Buttons. Spezielle "Nur Lese-Modus" Banner für Admins oder abgeschlossene Dokumente.
* **Unterstützte Sprachen:** Englisch, Deutsch, Französisch (sowohl als Quell- als auch als Zielsprache wählbar).

## 3. Rollen und Berechtigungen (RBAC)
Das System nutzt Role-Based Access Control (RBAC) mit drei spezifischen Rollen:

| Rolle | Zugriffsrechte |
| :--- | :--- |
| **Admin** | Zugriff auf alle Dokumente im System, allerdings im strikten **Read-Only Modus** (inkl. visuellem Hinweis). Kann Dokumente löschen, aber nicht verändern. Exklusiver Zugriff auf die Benutzerverwaltung (CRUD für User). Ein Admin kann sich nicht selbst löschen. |
| **User** | Darf neue Übersetzungsdokumente erstellen und den Text übersetzen. Darf das Dokument einem spezifischen Reviewer mit Fristsetzung zuweisen. Nur Lese-Zugriff, wenn das Dokument `IN_PRUEFUNG` ist. Kann eigene Dokumente löschen. Kein Zugriff auf die Benutzerverwaltung. |
| **Reviewer** | Nur Lese- und Schreibzugriff auf Dokumente, die ihm explizit zugewiesen wurden. Darf diese editieren und den Status ändern (z.B. zurück auf `KORREKTUR` oder auf `ERLEDIGT`). |

## 4. Dokumenten-Workflow und Status
Jedes Übersetzungsdokument durchläuft einen definierten Lebenszyklus mit folgenden Statuswerten:

| Status | Beschreibung |
| :--- | :--- |
| **OFFEN** | Ein neues Dokument wurde erstellt, aber die Übersetzung ist noch nicht abgeschlossen. |
| **IN_PRUEFUNG** | Der "User" hat die Übersetzung beendet und das Dokument einem "Reviewer" zur finalen Freigabe zugewiesen. |
| **KORREKTUR** | Der "Reviewer" fordert Änderungen am Dokument an. |
| **ERLEDIGT** | Der "Reviewer" hat das Dokument abgenommen. Es ist danach für alle Rollen schreibgeschützt (Read-Only). |

## 5. Externe API-Integration (Automatische Vorübersetzung)
* **Tool:** DeepL API Free (REST API).
* **Backend-Architektur:** Ein Spring Boot Service (`DeepLService`) sendet die REST-Anfragen an die DeepL API. Der API-Key ist in den `application.properties` hinterlegt. Bei Tests wird der Service per `@TestConfiguration` gemockt, um Kosten/Limitierungen zu vermeiden.
* **Frontend UI-Interaktion:**
  * Die Vorübersetzung wird durch den "Automatisch Übersetzen" Button ausgelöst.
  * Beim Klick wird der Button blockiert (Ladezustand).
  * Das Backend kommuniziert mit DeepL und füllt die rechte Seite (Übersetzung).
  * Ein maschinell übersetztes Dokument verbleibt zunächst im Status `OFFEN`.

## 6. Reviewer-Zuweisung (Workflow)
* **UI-Element:** Ein Bereich "Reviewer-Zuweisung" im Editor, sichtbar für "Users".
* **Datenquelle:** Dynamisch befüllt mit allen Benutzern der Rolle `REVIEWER`.
* **Ablauf:** Der "User" wählt den Reviewer, definiert eine **Deadline** (Datum/Uhrzeit) und klickt auf Speichern. Das Backend verknüpft die `reviewer_id` und ändert den Status auf `IN_PRUEFUNG`. Danach ist das Dokument für den "User" schreibgeschützt.

## 7. Löschberechtigungen für Dokumente & Benutzer
* **Admin:** Kann jedes Dokument und jeden Benutzer (außer sich selbst) im System jederzeit löschen. Bei Löschung eines Benutzers werden dank kaskadierenden Lösch-Befehlen in der Datenbank auch alle von diesem Benutzer erstellten Dokumente sowie seine Zuordnungen als Reviewer sauber entfernt.
* **User:** Darf ausschließlich Dokumente löschen, die er selbst erstellt hat (Prüfung der `creator_id`).
* **Frontend:** Lösch-Icons (Papierkorb) werden nur gerendert, wenn der User die entsprechende Berechtigung besitzt.

## 8. Dashboards und Listenansichten
Nach dem Login sehen Benutzer ein rollenspezifisches Dashboard mit einer Datentabelle.

* **Gemeinsame Spalten:** Titel/Auszug, Quell-/Zielsprache, Status (farbige Badges), Deadline (falls zutreffend), Aktionen.
* **Admin Dashboard:** Zeigt alle Dokumente an (mit Such- und Filterfunktionen) sowie einen separaten Bereich für die Verwaltung aller Systembenutzer. Keine Berechtigung, neue Dokumente zu erstellen.
* **User Dashboard:** Zeigt nur Dokumente mit eigener `creator_id`. Button zum Erstellen neuer Dokumente vorhanden.
* **Reviewer Dashboard:** Zeigt nur Dokumente mit eigener `reviewer_id`.

## 9. Datenmodell

### User
* `id` (UUID)
* `username` (String, unique)
* `password_hash` (String)
* `role` (Enum: ADMIN, USER, REVIEWER)

### Document
* `id` (UUID)
* `title` (String)
* `original_text` (Text)
* `translated_text` (Text)
* `source_language` (String)
* `target_language` (String)
* `status` (Enum: OFFEN, IN_PRUEFUNG, KORREKTUR, ERLEDIGT)
* `is_auto_translated` (Boolean)
* `created_at` (Timestamp)
* `creator_id` (Foreign Key -> User)
* `reviewer_id` (Foreign Key -> User, nullable)
* `review_deadline` (Timestamp, nullable)

## 10. Testing & Qualitätssicherung
* **Unit Tests:** Schnelle Tests unter Verwendung von "Fake Repositories" (z.B. `FakeDocumentRepository`), um Controller-Logik, RBAC und Workflows ohne echte Datenbankverbindung im Millisekundenbereich zu testen.
* **Integration Tests:** Umfassende End-to-End-Tests (`DocumentIntegrationTest`, `UserIntegrationTest`, `TranslationIntegrationTest`), welche die App mit einer echten PostgreSQL-Testdatenbank (`translation_db_test`) hochfahren und REST-Aufrufe sowie Datenbank-Speicherungen validieren. Diese laufen durch ein spezielles `@ActiveProfiles("test")` völlig unabhängig von der Entwicklungs-Datenbank.