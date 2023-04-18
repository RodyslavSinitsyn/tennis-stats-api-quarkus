package org.rsinitsyn.service;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MailService {

    @Inject
    Mailer mailer;

    public void sendTestMessage(String toMail) {
        runAsync(() -> mailer.send(Mail.withText(
                toMail,
                "Tennis Stats API",
                "Test message!!!")
        ));
    }

    private void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(runnable)
                .thenRun(() -> Log.info("Mail sent"));
    }
}
