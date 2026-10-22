package br.com.marealta;

import br.com.marealta.domain.ReportService;
import br.com.marealta.domain.ReservationService;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.web.Api;
import io.javalin.Javalin;

/** Monta o sistema: banco, repositórios, serviços e API. */
public final class Application {
    private Application() {
    }

    public static Javalin start(Database db, int port) {
        db.initSchema();
        ReservationRepository reservations = new ReservationRepository(db);
        ReservationService service = new ReservationService(db, reservations, new MessageRepository(db));
        return new Api(service, new ReportService(reservations)).create().start(port);
    }
}
