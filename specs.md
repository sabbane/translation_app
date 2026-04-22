# Projekt Spezifikation: Übersetzungs- und Review-Tool

## 1. Technologie-Stack
* **Backend:** Java, Spring Boot 3.4.3 (REST API)
* **Frontend:** React, TypeScript (Vite)
* **Styling:** CSS3 mit CSS-Variablen für Themes und **RTL-Unterstützung (Right-To-Left)** für Arabisch.
* **Bibliotheken:** Mammoth (DOCX Import), docx (DOCX Export), Lucide-React (Icons), Axios, i18next (Lokalisierung)
* **Datenbank:** PostgreSQL 18
    * **Entwicklung/Produktion:** `translation_db`
    * **Automatisierte Tests:** `translation_db_test` (Isoliert)
* **Authentifizierung:** Spring Security mit JWT (JSON Web Tokens)
* **Testing:** JUnit 5, Mockito, Playwright (E2E), Integrationstests gegen PostgreSQL.

## 2. Kernfunktionalität & UI
* **Split-Screen-Editor:** Zweigeteiltes Textfeld für Original (Links) und Übersetzung (Rechts).
* **Datei Import/Export:** Import von `.txt` und `.docx`, Export als `.docx`.
* **Multilingualität & RTL:** Vollständige Unterstützung für **Deutsch, Englisch, Französisch und Arabisch**. Das Layout wechselt bei Arabisch automatisch in den **RTL-Modus** (Spiegelung der UI).
* **Übersetzte Workflow-Status:** Alle Dokumenten-Status (Entwurf, In Prüfung, etc.) werden dynamisch in der gewählten Systemsprache angezeigt.
* **UX & Feedback:** Toast-Benachrichtigungen und animierte Dashboard-Übergänge.

## 3. Rollen und Berechtigungen (RBAC)
| Rolle | Zugriffsrechte |
| :--- | :--- |
| **Admin** | Zugriff auf alle Dokumente (Read-Only). Benutzerverwaltung (CRUD). Kann sich nicht selbst löschen. |
| **User** | Erstellen und Übersetzen. Zuweisung an Reviewer mit Deadline. Nur Lese-Zugriff bei Status `IN_PRUEFUNG` oder `ERLEDIGT`. |
| **Reviewer** | Zugriff auf zugewiesene Dokumente. Bearbeitung und Statusänderung (`KORREKTUR` oder `ERLEDIGT`). |

## 4. Dokumenten-Workflow und Status (Lokalisiert)
Die Statuswerte werden in der Benutzeroberfläche übersetzt dargestellt:

| Interner Status | DE | EN | FR | AR |
| :--- | :--- | :--- | :--- | :--- |
| **OFFEN** | Entwurf | Draft | Brouillon | مسودة |
| **IN_PRUEFUNG** | In Prüfung | In Review | En révision | قيد المراجعة |
| **KORREKTUR** | Korrektur | Correction | Correction | تصحيح |
| **ERLEDIGT** | Fertig | Done | Terminé | تم |

## 5. Externe API-Integration (Auto-Translation)
* **Tool:** DeepL API.
* **Logik:** Gekapselt im `DeepLService`. API-Key über Umgebungsvariable `DEEPL_API_KEY`.
* **Testing:** In Tests wird die API per `@Primary` Mock-Service übersteuert, um Kosten und Abhängigkeiten zu minimieren.

## 6. Reviewer-Zuweisung
* User wählt einen Reviewer aus der Liste und setzt eine Deadline.
* Status wechselt auf `IN_PRUEFUNG`.
* Das Dokument wird für den Creator gesperrt (Read-Only), solange es beim Reviewer liegt.

## 7. Löschberechtigungen & Datenbankintegrität
* **Kaskadierung:** Beim Löschen eines Users werden durch die Datenbank-Constraints alle zugehörigen Dokumente automatisch entfernt (Vermeidung von Datenleichen).
* **Berechtigungsprüfung:** Sowohl im Frontend (UI-Elemente ausgeblendet) als auch im Backend (Security-Check) abgesichert.

## 8. Dashboards
* **Ansichten:** Umschaltbar zwischen Tabellen-Ansicht und Kachel-Ansicht (Grid).
* **Grid-Layout:** Responsives 4-Spalten-Layout mit Icons (Brille für Review, Siegel für Erledigt).
* **Filter:** Suche nach Text, Status-Filter und Sprachkombinations-Filter (z.B. DE -> EN).

## 9. Datenmodell (Auszug)
* **User:** `id`, `username`, `password_hash`, `role`.
* **Document:** `id`, `title`, `original_text`, `translated_text`, `source_language`, `target_language`, `status`, `creator_id`, `reviewer_id`, `review_deadline`.

## 10. Qualitätssicherung & Test-Infrastruktur
* **Datenbank-Isolation:** Playwright und JUnit Tests nutzen ausschließlich die `translation_db_test`.
* **Profile-aware DataInitializer:** Der `DataInitializer` befüllt die Datenbank nur im `test`-Profil mit Dummy-Daten und sorgt dort für einen Clean-State. Im Standard-Profil werden bestehende Daten geschützt.
* **Regression Testing:** Playwright-Suite deckt Login, Workflow, UX-Toggles und Security ab. Das Benutzerhandbuch-Popup wird in Tests automatisch unterdrückt (`disableManual` Flag).

## 11. Benutzerhandbuch (User Manual)
* **Interaktives Popup:** Erscheint einmalig nach dem Login (oder manuell aufrufbar).
* **Sprachwahl:** Der Inhalt passt sich der gewählten i18n-Sprache an (DE, EN, FR, AR).
* **Inhalt:** Rollenspezifische Workflows und App-Übersicht.
* **Persistenz:** Ein `localStorage`-Mechanismus stellt sicher, dass das Popup erst nach dem Schließen als "gelesen" markiert wird (verhindert Verschwinden durch Race-Conditions).n, Abschließen).
* **Sprachen:** Deutsch, Englisch, Französisch und Arabisch.