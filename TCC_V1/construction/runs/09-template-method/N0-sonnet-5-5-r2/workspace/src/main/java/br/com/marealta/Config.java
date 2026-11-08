package br.com.marealta;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.util.List;

public record Config(int port, String dbUrl, String dbUser, String dbPassword, BigDecimal tourismTaxPerGuestNight) {

    /** Os quartos da pousada. */
    public static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(102, RoomType.STANDARD, 2, Money.of("249.90")),
            new Room(201, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(202, RoomType.DELUXE, 3, Money.of("389.90")),
            new Room(301, RoomType.SUITE, 4, Money.of("579.90")));

    /** Taxa de turismo da prefeitura por hóspede por noite (muda todo ano: TOURISM_TAX_PER_GUEST_NIGHT). */
    public static final String DEFAULT_TOURISM_TAX = "4.50";

    public static Config fromEnv() {
        String port = System.getenv("PORT");
        String tax = System.getenv("TOURISM_TAX_PER_GUEST_NIGHT");
        return new Config(
                port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()),
                require("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"),
                Money.of(tax == null || tax.isBlank() ? DEFAULT_TOURISM_TAX : tax.trim()));
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}
