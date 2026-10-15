package com.loja.persistencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.loja.dominio.Persistencia;
import com.loja.dominio.PersistenciaMemoria;

@Configuration
public class ConfigPersistencia {

    private static final Logger log = LoggerFactory.getLogger(ConfigPersistencia.class);

    @Bean
    Persistencia persistencia(@Value("${app.db.url:}") String url, @Value("${app.db.user:}") String usuario,
                              @Value("${app.db.password:}") String senha) {
        if (url.isBlank()) {
            log.warn("DB_URL não definido: os dados ficarão só em memória e serão perdidos ao reiniciar");
            return new PersistenciaMemoria();
        }
        return new PersistenciaComEspera(new PgPersistencia(new PgConexao(url, usuario, senha)));
    }

    /** Dá algum tempo para o banco subir junto com o serviço antes de desistir. */
    private static final class PersistenciaComEspera implements Persistencia {

        private static final int TENTATIVAS = 30;

        private final Persistencia destino;

        PersistenciaComEspera(Persistencia destino) {
            this.destino = destino;
        }

        @Override
        public com.loja.dominio.Estado carregar() {
            RuntimeException ultimo = null;
            for (int i = 1; i <= TENTATIVAS; i++) {
                try {
                    return destino.carregar();
                } catch (RuntimeException e) {
                    ultimo = e;
                    log.warn("Banco indisponível (tentativa {}/{}): {}", i, TENTATIVAS, e.getMessage());
                    try {
                        Thread.sleep(2_000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            throw ultimo;
        }

        @Override
        public void gravar(com.loja.dominio.Mudancas mudancas) {
            destino.gravar(mudancas);
        }
    }
}
