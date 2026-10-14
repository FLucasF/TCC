package br.com.loja.pedidos.comum;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class Configuracao {

    @Bean
    public DataSource dataSource(@Value("${DB_URL}") String url,
                                 @Value("${DB_USER:}") String usuario,
                                 @Value("${DB_PASSWORD:}") String senha) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(urlJdbc(url));
        if (!usuario.isBlank()) {
            dataSource.setUsername(usuario);
        }
        if (!senha.isEmpty()) {
            dataSource.setPassword(senha);
        }
        return dataSource;
    }

    /**
     * Aceita o endereço do banco tanto no formato JDBC ("jdbc:postgresql://host:5432/banco")
     * quanto no formato "postgresql://host:5432/banco" ou só "host:5432/banco".
     */
    static String urlJdbc(String url) {
        String limpa = url.trim();
        if (limpa.startsWith("jdbc:")) {
            return limpa;
        }
        String semEsquema = limpa.replaceFirst("^(postgres|postgresql)://", "");
        int arroba = semEsquema.indexOf('@');
        int barra = semEsquema.indexOf('/');
        if (arroba >= 0 && (barra < 0 || arroba < barra)) {
            semEsquema = semEsquema.substring(arroba + 1);
        }
        return "jdbc:postgresql://" + semEsquema;
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager gerenciador) {
        return new TransactionTemplate(gerenciador);
    }

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
