# Translation App

Eine Full-Stack Web-Anwendung zur Verwaltung und automatischen Übersetzung von Dokumenten mittels DeepL API.

## Features

- **Dokumentenverwaltung**: Erstellen, Bearbeiten und Löschen von Dokumenten.
- **Automatische Übersetzung**: Integration der DeepL API zur automatischen Übersetzung von Texten.
- **Benutzerverwaltung (RBAC)**:
  - **ADMIN**: Voller Zugriff auf alle Dokumente und Benutzerverwaltung.
  - **REVIEWER**: Überprüfung und Bestätigung von Übersetzungen.
  - **USER**: Erstellen und Verwalten eigener Dokumente.
- **Sicherheit**: Authentifizierung via JWT und Passwort-Hashing (BCrypt).

## Technologien

### Backend
- Java 21+ / Spring Boot 3.4
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL (oder H2 für lokale Tests)
- Gradle

### Frontend
- React 18
- Vite
- TypeScript
- Axios
- CSS3 (Vanilla CSS)

## Installation & Start

### 1. Datenbank vorbereiten
Stelle sicher, dass eine PostgreSQL-Datenbank namens `translation_db` existiert oder passe die `src/main/resources/application.properties` an.

### 2. API-Key konfigurieren
Trage deinen DeepL API-Key in `src/main/resources/application.properties` ein:
```properties
deepl.api.key=DEIN_DEEPL_KEY_HIER
```

### 3. Backend starten
```bash
./gradlew bootRun
```

### 4. Frontend starten
```bash
cd frontend
npm install
npm run dev
```

## Standard-Benutzer (Initialisierung)
Beim ersten Start werden folgende Testbenutzer angelegt:
- **Admin**: `admin` / `admin123`
- **User**: `user` / `user123`
- **Reviewer**: `reviewer` / `reviewer123`
