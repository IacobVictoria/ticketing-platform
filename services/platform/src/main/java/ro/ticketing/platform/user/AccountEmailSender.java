package ro.ticketing.platform.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Sends the emails about an account. Locally they land in Mailpit: http://localhost:8025
 * Each one runs after the transaction commits, so an email never carries a token that was rolled back, and in the
 * background, so a slow mail server holds neither a request nor a database connection.
 * If the mail server is down we only log it: the user can register again or ask for a new email.
 */
@Component
class AccountEmailSender {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailSender.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String from;

    AccountEmailSender(JavaMailSender mailSender,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail-from}") String from) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    @Async
    @TransactionalEventListener
    void sendVerification(VerificationEmailRequested event) {
        String link = frontendUrl + "/verify-email?token=" + event.rawToken();
        send(event.userId(), event.email(), "Confirmă adresa de email", """
                Bun venit!

                Ca să-ți activezi contul, deschide link-ul de mai jos (valabil 24 de ore)
                și introdu parola aleasă la înregistrare:
                %s

                Dacă nu tu ți-ai făcut cont, ignoră acest email.
                """.formatted(link));
    }

    /** Register answers the same for every email, so the owner of an existing account learns about it here. */
    @Async
    @TransactionalEventListener
    void sendExistingAccount(ExistingAccountEmailRequested event) {
        String link = frontendUrl + "/login";
        send(event.userId(), event.email(), "Ai deja un cont", """
                Bună!

                Cineva a încercat să-și facă un cont nou cu această adresă de email,
                dar ai deja un cont confirmat. Te poți autentifica aici:
                %s

                Dacă nu ai fost tu, poți ignora acest email: contul tău nu a fost modificat.
                """.formatted(link));
    }

    private void send(UUID userId, String email, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Could not send the email \"{}\" for user {}: {}", subject, userId, e.getMessage());
        }
    }
}
