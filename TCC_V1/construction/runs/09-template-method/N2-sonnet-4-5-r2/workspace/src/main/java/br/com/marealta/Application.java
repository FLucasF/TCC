package br.com.marealta;

import br.com.marealta.api.RestApi;
import br.com.marealta.repository.*;
import br.com.marealta.service.*;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        RoomRepository roomRepository = new RoomRepositoryImpl(database.getDataSource());
        ReservationRepository reservationRepository = new ReservationRepositoryImpl(database.getDataSource());
        MessageRepository messageRepository = new MessageRepositoryImpl(database.getDataSource());

        roomRepository.initialize();

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService();
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                roomRepository,
                pricingService,
                documentService,
                messageService
        );

        RestApi restApi = new RestApi(
                roomRepository,
                reservationRepository,
                messageRepository,
                reservationService,
                messageService
        );

        Javalin app = restApi.createApp();
        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }
}
