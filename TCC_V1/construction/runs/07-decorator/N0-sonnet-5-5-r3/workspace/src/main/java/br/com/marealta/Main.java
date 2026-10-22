package br.com.marealta;

import br.com.marealta.domain.Reports;
import br.com.marealta.domain.ReservationService;
import br.com.marealta.mail.MailSettings;
import br.com.marealta.mail.Mailer;
import br.com.marealta.web.Api;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        Db db = new Db(require(env, "DB_URL"), env.getOrDefault("DB_USER", ""), env.getOrDefault("DB_PASSWORD", ""));
        db.init();
        MailSettings mail = MailSettings.fromEnv(env);
        ReservationService service = new ReservationService(db, new Mailer(mail));
        int port = Integer.parseInt(env.getOrDefault("PORT", "7070"));
        new Api(db, service, new Reports(db)).build().start(port);
        System.out.println("Pousada Maré Alta no ar na porta " + port + (mail.testMode() ? " (MODO DE TESTE)" : ""));
    }

    private static String require(Map<String, String> env, String name) {
        String v = env.get(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente " + name + " é obrigatória");
        return v;
    }
}
