package br.com.marealta;

import br.com.marealta.messaging.*;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.PostgresMessageLog;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Routes;

import java.util.List;
import javax.sql.DataSource;

public class Main {
    public static void main(String[] args) {
        DataSource ds = Database.connect(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        boolean archiveCopy = !"false".equalsIgnoreCase(System.getenv("HOTEL_ARCHIVE_COPY"));

        MessageLog log = new PostgresMessageLog(ds);
        ReservationRepository reservations = new ReservationRepository(ds);
        ReservationService service = new ReservationService(reservations,
                messageSender(log, testMode, archiveCopy));
        new Routes(service, reservations, log).start(port);
    }

    /**
     * Único lugar que decide as regras de mensagens desta instalação, da mais externa para a mais interna.
     * Uma regra nova (horário, limite por hóspede...) entra aqui, com o escopo em que vale.
     */
    static MessageSender messageSender(MessageLog log, boolean testMode, boolean archiveCopy) {
        List<MessageRule> rules = new java.util.ArrayList<>();
        rules.add(new MessageRule(RuleScope.ALWAYS, next -> new BlockInvalidAddress(next, log)));
        if (archiveCopy) {
            rules.add(new MessageRule(RuleScope.OUTSIDE_TEST_MODE_ONLY, ArchiveCopy::new));
        }
        rules.add(new MessageRule(RuleScope.TEST_MODE_ONLY, RedirectToTest::new));
        return MessagePipeline.build(testMode, new LogSender(log), rules);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Variável de ambiente ausente: " + name);
        return value;
    }
}
