package br.com.marealta;

import br.com.marealta.infra.Database;
import br.com.marealta.infra.MessageRepository;
import br.com.marealta.infra.ReservationRepository;
import br.com.marealta.plan.CorporatePlan;
import br.com.marealta.plan.LongStayPlan;
import br.com.marealta.plan.NonRefundablePlan;
import br.com.marealta.plan.RatePlans;
import br.com.marealta.plan.StandardPlan;
import br.com.marealta.plan.WeekendPackagePlan;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.List;

public class App {
    public static void main(String[] args) {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.migrate();

        // Novo plano de tarifa: uma classe nova e uma linha aqui.
        RatePlans plans = new RatePlans(List.of(
                new StandardPlan(),
                new NonRefundablePlan(),
                new CorporatePlan(),
                new WeekendPackagePlan(),
                new LongStayPlan()));

        ReservationRepository reservationRepo = new ReservationRepository();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Routes routes = new Routes(
                new ReservationService(db, reservationRepo, new MessageRepository(), plans),
                new ReportService(db, reservationRepo),
                mapper);

        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));
        routes.register(app);
        String port = System.getenv("PORT");
        app.start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
