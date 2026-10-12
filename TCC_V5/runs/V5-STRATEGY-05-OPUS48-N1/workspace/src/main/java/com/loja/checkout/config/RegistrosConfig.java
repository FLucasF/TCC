package com.loja.checkout.config;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.comum.Registro;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Junta cada familia de casos num registro. Como o Spring injeta a lista de
 * todas as implementacoes, basta criar uma classe nova para a opcao nova
 * aparecer aqui sozinha.
 */
@Configuration
public class RegistrosConfig {

    @Bean
    Registro<ModalidadeEntrega> entregas(List<ModalidadeEntrega> casos) {
        return new Registro<>(casos);
    }

    @Bean
    Registro<Cupom> cupons(List<Cupom> casos) {
        return new Registro<>(casos);
    }

    @Bean
    Registro<NivelClube> niveisClube(List<NivelClube> casos) {
        return new Registro<>(casos);
    }

    @Bean
    Registro<FormaPagamento> pagamentos(List<FormaPagamento> casos) {
        return new Registro<>(casos);
    }
}
