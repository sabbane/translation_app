package com.translationapp;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;

import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.Disabled;

@SpringBootTest
@Disabled("Run this test manually to seed the dev database. Uncomment when needed.")
public class DatabaseSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Test
    @Commit
    public void seedDatabase() {
        System.out.println("Starting database seed with 100% realistic translations...");
        
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        User admin = createUser("admin", "admin123", Role.ADMIN);
        User user1 = createUser("user-1", "user123", Role.USER);
        User user2 = createUser("user-2", "user123", Role.USER);
        
        User revDE = createUser("reviewer-1", "reviewer123", Role.REVIEWER);
        User revEN = createUser("reviewer-2", "reviewer123", Role.REVIEWER);
        User revFR = createUser("reviewer-3", "reviewer123", Role.REVIEWER);

        // 2. Data Pools
        // Format: {Title, SourceLang, TargetLang, Original, Translation}
        String[][] pairs = {
            {"Q1 Geschäftsbericht", "DE", "EN", "Unser Umsatz stieg im ersten Quartal um 15%, getrieben durch starke Verkäufe in Europa.", "Our revenue increased by 15% in the first quarter, driven by strong sales in Europe."},
            {"Sicherheitsupdate", "DE", "EN", "Bitte installieren Sie das neueste Sicherheitsupdate, um Ihre Daten vor unbefugtem Zugriff zu schützen.", "Please install the latest security update to protect your data from unauthorized access."},
            {"Urlaubsregelung", "DE", "FR", "Mitarbeiter haben Anspruch auf 30 Tage bezahlten Urlaub pro Kalenderjahr.", "Les employés ont droit à 30 jours de congés payés par année civile."},
            {"Marketing Plan", "EN", "DE", "The new campaign will focus on social media engagement and influencer partnerships.", "Die neue Kampagne wird sich auf Social-Media-Engagement und Influencer-Partnerschaften konzentrieren."},
            {"Privacy Policy", "EN", "DE", "We value your privacy and only store data necessary for the service.", "Wir schätzen Ihre Privatsphäre und speichern nur Daten, die für den Dienst erforderlich sind."},
            {"Contrat de Travail", "FR", "DE", "Le présent contrat est conclu pour une durée indéterminée.", "Dieser Vertrag wird auf unbestimmte Zeit geschlossen."},
            {"Projekt Alpha", "DE", "EN", "Die Deadline für Projekt Alpha wurde auf Ende nächsten Monats verschoben.", "The deadline for Project Alpha has been moved to the end of next month."},
            {"IT Support", "EN", "DE", "If you experience any technical issues, please contact our 24/7 helpdesk.", "Falls Sie technische Probleme haben, kontaktieren Sie bitte unseren 24/7 Helpdesk."},
            {"Budget 2024", "DE", "FR", "Das Budget für Forschung wurde um eine Million Euro erhöht.", "Le budget de la recherche a été augmenté d'un million d'euros."},
            {"Feedback Runde", "DE", "EN", "Wir schätzen Ihr Feedback sehr und werden es in die nächste Version einbauen.", "We highly value your feedback and will incorporate it into the next version."},
            {"Server Migration", "EN", "DE", "The server migration is scheduled for Sunday at 2:00 AM to minimize downtime.", "Die Server-Migration ist für Sonntag um 2:00 Uhr morgens geplant, um Ausfallzeiten zu minimieren."},
            {"Vente Flash", "FR", "DE", "Profitez de nos offres exceptionnelles pendant cette vente flash exclusive.", "Profitieren Sie von unseren außergewöhnlichen Angeboten während dieses exklusiven Flash-Sales."},
            {"Team Meeting", "DE", "EN", "Das nächste Team-Meeting findet im Konferenzraum B statt.", "The next team meeting will take place in conference room B."},
            {"Compliance Info", "EN", "DE", "All employees must complete the annual compliance training by Friday.", "Alle Mitarbeiter müssen das jährliche Compliance-Training bis Freitag absolvieren."},
            {"Expansion Asien", "DE", "EN", "Wir planen die Eröffnung einer neuen Niederlassung in Tokio im nächsten Jahr.", "We plan to open a new branch in Tokyo next year."},
            {"User Manual", "EN", "DE", "Read the user manual carefully before using the device for the first time.", "Lesen Sie das Benutzerhandbuch sorgfältig durch, bevor Sie das Gerät zum ersten Mal benutzen."},
            {"Logistik Plan", "DE", "FR", "Die Lieferungen werden ab nächster Woche über das neue Logistikzentrum abgewickelt.", "Les livraisons seront traitées via le nouveau centre logistique à partir de la semaine prochaine."},
            {"Code of Conduct", "EN", "DE", "Respect and integrity are the core values of our company's code of conduct.", "Respekt und Integrität sind die Grundwerte des Verhaltenskodex unseres Unternehmens."},
            {"Wartung Klimaanlage", "DE", "EN", "Die Wartung der Klimaanlage erfolgt am Mittwoch zwischen 9 und 12 Uhr.", "Air conditioning maintenance will take place on Wednesday between 9 AM and 12 PM."},
            {"Rapport Annuel", "FR", "DE", "Le rapport annuel détaille nos performances financières et sociales.", "Der Jahresbericht beschreibt unsere finanzielle und soziale Leistung."},
            {"Büroreinigung", "DE", "EN", "Ab sofort findet die Büroreinigung täglich ab 18 Uhr statt.", "From now on, office cleaning will take place daily from 6 PM."},
            {"Cloud Storage", "EN", "DE", "Your cloud storage is 90% full. Please upgrade your plan soon.", "Ihr Cloud-Speicher ist zu 90% voll. Bitte aktualisieren Sie bald Ihren Plan."},
            {"Arbeitssicherheit", "DE", "FR", "Das Tragen von Schutzhelmen ist auf der gesamten Baustelle Pflicht.", "Le port du casque est obligatoire sur tout le chantier."},
            {"Onboarding Guide", "EN", "DE", "This guide will help you navigate your first week at the company.", "Dieser Leitfaden wird Ihnen helfen, sich in Ihrer ersten Woche im Unternehmen zurechtzufinden."},
            {"Mitarbeiterfest", "DE", "EN", "Wir laden alle herzlich zu unserem Mitarbeiterfest am Freitagabend ein.", "We cordially invite everyone to our employee party on Friday evening."},
            {"Update v2.1", "EN", "DE", "Version 2.1 includes several bug fixes and a new dark mode option.", "Version 2.1 enthält mehrere Fehlerbehebungen und eine neue Dark-Mode-Option."},
            {"Recherche Bio", "FR", "DE", "La recherche en biotechnologie progresse rapidement dans nos laboratoires.", "Die Forschung in der Biotechnologie schreitet in unseren Laboren schnell voran."},
            {"Neue Kaffeemaschine", "DE", "EN", "In der Küche steht nun eine neue Kaffeemaschine für alle bereit.", "There is now a new coffee machine in the kitchen for everyone."},
            {"Software Lizenz", "EN", "DE", "The software license must be renewed before it expires next month.", "Die Softwarelizenz muss erneuert werden, bevor sie nächsten Monat abläuft."},
            {"Direction Stratégique", "FR", "DE", "La direction stratégique se concentre sur l'innovation et la durabilité.", "Die strategische Ausrichtung konzentriert sich auf Innovation und Nachhaltigkeit."},
            {"Besuchergruppe", "DE", "EN", "Morgen wird eine Besuchergruppe aus den USA unser Werk besichtigen.", "Tomorrow a visitor group from the USA will visit our plant."}
        };

        // 3. Seed user-1 (14 Docs)
        for (int i = 0; i < 14; i++) {
            seedPair(user1, pairs[i], i, revDE, revEN, revFR);
        }

        // 4. Seed user-2 (17 Docs)
        for (int i = 0; i < 17; i++) {
            // We use the remaining pairs and wrap around if necessary
            seedPair(user2, pairs[(i + 14) % pairs.length], i, revDE, revEN, revFR);
        }

        System.out.println("Database seeded with 31 realistic translation pairs!");
    }

    private void seedPair(User creator, String[] pair, int index, User rDE, User rEN, User rFR) {
        String title = pair[0];
        String sLang = pair[1];
        String tLang = pair[2];
        String oText = pair[3];
        String tText = pair[4];

        // Varied Status
        DocumentStatus status = DocumentStatus.values()[index % 4];
        
        // Match reviewer to target language
        User reviewer = tLang.equals("DE") ? rDE : (tLang.equals("EN") ? rEN : rFR);
        
        // Random Deadline: vary between -12 and +20 days to get a good mix
        int randomDays = random.nextInt(32) - 12;
        LocalDateTime deadline = LocalDateTime.now().plusDays(randomDays);

        createDoc(creator, title, oText, 
            (status == DocumentStatus.OFFEN ? null : tText),
            sLang, tLang, status, 
            (status == DocumentStatus.OFFEN ? null : reviewer), 
            (status == DocumentStatus.OFFEN ? null : deadline));
    }

    private User createUser(String username, String password, Role role) {
        User user = new User(username, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    private void createDoc(User creator, String title, String originalText, String translatedText, String sourceLang, String targetLang, 
                           DocumentStatus status, User reviewer, LocalDateTime deadline) {
        Document doc = new Document();
        doc.setCreator(creator);
        doc.setTitle(title);
        doc.setOriginalText(originalText);
        doc.setTranslatedText(translatedText);
        doc.setSourceLanguage(sourceLang);
        doc.setTargetLanguage(targetLang);
        doc.setStatus(status);
        if (reviewer != null) {
            doc.setReviewer(reviewer);
            doc.setReviewDeadline(deadline);
        }
        documentRepository.save(doc);
    }
}
