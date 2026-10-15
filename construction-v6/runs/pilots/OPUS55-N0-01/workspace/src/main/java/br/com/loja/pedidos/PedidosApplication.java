package br.com.loja.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PedidosApplication {

    public static void main(String[] args) {
        aceitarEnderecoDoBancoSemPrefixoJdbc();
        SpringApplication.run(PedidosApplication.class, args);
    }

    /** Aceita DB_URL também no formato "postgresql://servidor:5432/banco", além do "jdbc:postgresql://...". */
    private static void aceitarEnderecoDoBancoSemPrefixoJdbc() {
        String url = System.getenv("DB_URL");
        if (url == null || url.startsWith("jdbc:")) {
            return;
        }
        if (url.startsWith("postgres://")) {
            url = "postgresql://" + url.substring("postgres://".length());
        }
        if (url.startsWith("postgresql://")) {
            System.setProperty("spring.datasource.url", "jdbc:" + url);
        }
    }
}
