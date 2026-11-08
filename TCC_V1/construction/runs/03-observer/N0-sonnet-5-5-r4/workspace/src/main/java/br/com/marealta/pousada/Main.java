package br.com.marealta.pousada;

import br.com.marealta.pousada.events.FinanceNotifications;
import br.com.marealta.pousada.events.GuestNotifications;
import br.com.marealta.pousada.events.HousekeepingNotifications;
import br.com.marealta.pousada.events.NotificationRule;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.web.Api;
import io.javalin.Javalin;
import java.util.List;

public final class Main {

    private Main() {
    }

    /**
     * Quem é avisado dos eventos de reserva. Para incluir o restaurante, o transfer ou qualquer outra área,
     * crie uma {@link NotificationRule} e acrescente aqui; para tirar, remova daqui.
     */
    public static List<NotificationRule> notificationRules() {
        return List.of(
                new GuestNotifications(),
                new HousekeepingNotifications("governanca@pousadamarealta.com.br"),
                new FinanceNotifications("financeiro@pousadamarealta.com.br"));
    }

    public static Javalin createApp(Database db) {
        db.migrate();
        return new Api(new ReservationService(db, notificationRules()), new ReportService(db)).create();
    }

    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("A variável DB_URL é obrigatória");
        }
        Database db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"), null);
        String port = System.getenv("PORT");
        createApp(db).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
