package br.com.marealta;

import java.math.BigDecimal;
import java.util.Map;

/** Runtime settings, read from the environment. */
public record Config(int port, String dbUrl, String dbUser, String dbPassword, BigDecimal tourismTaxPerGuestNight) {

    public static final BigDecimal DEFAULT_TOURISM_TAX = new BigDecimal("4.50");

    public static Config from(Map<String, String> env) {
        String port = env.get("PORT");
        String tax = env.get("TOURISM_TAX_PER_GUEST_NIGHT");
        return new Config(
                port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()),
                env.get("DB_URL"),
                env.get("DB_USER"),
                env.get("DB_PASSWORD"),
                tax == null || tax.isBlank() ? DEFAULT_TOURISM_TAX : new BigDecimal(tax.trim()));
    }
}
