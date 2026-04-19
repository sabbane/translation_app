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
import java.util.Optional;
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

    @Test
    @Commit
    public void seedDatabase() {
        System.out.println("Starting database seed...");
        
        // Clean up database (optional, but ensures a clean state)
        documentRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        User admin = createUser("admin", "admin123", Role.ADMIN);
        User user1 = createUser("user-1", "user123", Role.USER);
        User user2 = createUser("user-2", "user123", Role.USER);
        
        // Reviewers
        User reviewerDE = createUser("reviewer-1", "reviewer123", Role.REVIEWER); // DE
        User reviewerEN = createUser("reviewer-2", "reviewer123", Role.REVIEWER); // EN
        User reviewerFR = createUser("reviewer-3", "reviewer123", Role.REVIEWER); // FR

        // 2. Create Documents for user-1
        createDoc(user1, "Einführung neue Software", "Die Einführung der neuen Software wird im kommenden Quartal stattfinden. Alle Mitarbeiter werden gebeten, sich rechtzeitig in die Schulungstermine einzutragen. Wir erwarten eine reibungslose Umstellung auf das neue System.", 
            null,
            "DE", "EN", DocumentStatus.OFFEN, null, null);
            
        createDoc(user1, "Q3 Financial Results", "Unsere Q3 Finanzergebnisse zeigen eine deutliche Steigerung des Gesamtumsatzes. Wir schreiben diesen Erfolg der jüngsten Markteinführung unseres Vorzeigeprodukts auf dem europäischen Markt zu. Detaillierte Berichte werden nächste Woche verteilt.", 
            "Our Q3 financial results show a significant increase in overall revenue. We attribute this success to the recent launch of our flagship product in the European market. Detailed reports will be distributed next week.",
            "DE", "EN", DocumentStatus.IN_PRUEFUNG, reviewerEN, LocalDateTime.now().plusDays(2));
            
        createDoc(user1, "Nouveau Budget", "Der Vorstand hat das neue Budget für das kommende Jahr genehmigt. Die Investitionen in Forschung und Entwicklung werden um 15% erhöht. Das sind hervorragende Neuigkeiten für die Innovation.", 
            "Le comité de direction a approuvé le nouveau budget pour l'année prochaine. Les investissements dans la recherche et le développement seront augmentés de 15%. C'est une excellente nouvelle pour l'innovation.",
            "DE", "FR", DocumentStatus.IN_PRUEFUNG, reviewerFR, LocalDateTime.now().plusDays(1));
            
        createDoc(user1, "Wartungsarbeiten", "Dear customers, due to maintenance work, our service will be temporarily unavailable over the weekend. We ask for your understanding and will try to keep the downtime as short as possible.", 
            "Sehr geehrte Kunden, aufgrund von Wartungsarbeiten wird unser Service am Wochenende vorübergehend nicht erreichbar sein. Wir bitten um Ihr Verständnis und bemühen uns, die Ausfallzeit so kurz wie möglich zu halten.",
            "EN", "DE", DocumentStatus.KORREKTUR, reviewerDE, LocalDateTime.now().minusDays(1)); // Overdue
            
        createDoc(user1, "Company Retreat", "Willkommen zu unserem jährlichen Firmenausflug! Wir haben eine Reihe von Teambuilding-Aktivitäten und Workshops organisiert, um die Zusammenarbeit zu fördern. Bitte überprüfen Sie den Zeitplan und teilen Sie uns mit, ob Sie diätetische Einschränkungen haben.", 
            "Welcome to our annual company retreat! We have organized a series of team-building activities and workshops to foster collaboration. Please review the schedule and let us know if you have any dietary restrictions.",
            "DE", "EN", DocumentStatus.ERLEDIGT, reviewerEN, LocalDateTime.now().minusDays(5));
            
        createDoc(user1, "Événement de charité", "Wir laden Sie herzlich zu unserem jährlichen Wohltätigkeitsereignis ein. Die gesammelten Spenden kommen lokalen Umweltschutzorganisationen zugute. Kommen Sie zahlreich!", 
            null,
            "DE", "FR", DocumentStatus.OFFEN, null, null);

        // 3. Create Documents for user-2
        createDoc(user2, "Privacy Policy Update", "Die neue Datenschutzrichtlinie erläutert, wie wir Ihre persönlichen Daten sammeln, verwenden und schützen. Es ist wichtig, dass alle Benutzer diese Änderungen sorgfältig prüfen. Ihre fortgesetzte Nutzung der Plattform gilt als Zustimmung zu diesen Bedingungen.", 
            "The new privacy policy outlines how we collect, use, and protect your personal data. It is important that all users review these changes carefully. Your continued use of the platform constitutes agreement to these terms.",
            "DE", "EN", DocumentStatus.IN_PRUEFUNG, reviewerEN, LocalDateTime.now().plusDays(3));
            
        createDoc(user2, "Sicherheitsupdate", "The new update fixes several critical security vulnerabilities. We recommend all users to install the update immediately. In case of problems, please contact technical support.", 
            "Das neue Update behebt mehrere kritische Sicherheitslücken. Wir empfehlen allen Benutzern, die Aktualisierung umgehend zu installieren. Bei Problemen wenden Sie sich bitte an den technischen Support.",
            "EN", "DE", DocumentStatus.ERLEDIGT, reviewerDE, LocalDateTime.now().minusDays(10));
            
        createDoc(user2, "Analyse de marché", "Der Marktanalysebericht weist auf eine Veränderung der Verbrauchergewohnheiten hin. Wir müssen unsere Marketingstrategie entsprechend anpassen. Ein Planungstreffen ist für nächsten Dienstag geplant.", 
            "Le rapport d'analyse de marché indique un changement dans les habitudes des consommateurs. Nous devons adapter notre stratégie marketing en conséquence. Une réunion de planification est prévue mardi prochain.",
            "DE", "FR", DocumentStatus.KORREKTUR, reviewerFR, LocalDateTime.now().plusDays(2));
            
        createDoc(user2, "Neuer Arbeitsvertrag", "Please find attached the draft for the new employment contract. All essential changes regarding the vacation regulations are marked on page 3. We request feedback by Friday.", 
            "Bitte finden Sie anbei den Entwurf für den neuen Arbeitsvertrag. Alle wesentlichen Änderungen bezüglich der Urlaubsregelungen sind auf Seite 3 markiert. Wir bitten um Rückmeldung bis Freitag.",
            "EN", "DE", DocumentStatus.IN_PRUEFUNG, reviewerDE, LocalDateTime.now().plusDays(1));
            
        createDoc(user2, "Promotion Congrats", "Herzlichen Glückwunsch zu Ihrer Beförderung! Ihre harte Arbeit und Ihr Engagement wurden vom Management-Team anerkannt. Wir freuen uns darauf, Ihren weiteren Erfolg in Ihrer neuen Rolle zu sehen.", 
            null,
            "DE", "EN", DocumentStatus.OFFEN, null, null);
            
        createDoc(user2, "Retard de livraison", "Die Lieferung Ihrer Bestellung hat sich aufgrund logistischer Probleme verzögert. Wir bitten Sie, diese Unannehmlichkeiten zu entschuldigen. Eine neue Sendungsverfolgungsnummer wird Ihnen in Kürze mitgeteilt.", 
            "La livraison de votre commande a été retardée en raison de problèmes logistiques. Nous vous prions de nous excuser pour ce désagrément. Un nouveau numéro de suivi vous sera communiqué sous peu.",
            "DE", "FR", DocumentStatus.ERLEDIGT, reviewerFR, LocalDateTime.now().minusDays(2));

        System.out.println("Database seeded successfully!");
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
