package br.com.loja.checkout.aplicacao;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.loja.checkout.dominio.Registro;
import br.com.loja.checkout.dominio.clube.NivelClube;
import br.com.loja.checkout.dominio.cupom.Cupom;
import br.com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import br.com.loja.checkout.dominio.pagamento.FormaPagamento;

/**
 * Monta os registros a partir de tudo que o Spring encontra de cada eixo. Caso
 * novo (transportadora, cupom, nivel, forma de pagamento) entra sozinho aqui.
 */
@Configuration
public class RegistrosConfig {

    @Bean
    Registro<ModalidadeEntrega> modalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        return new Registro<>(modalidades);
    }

    @Bean
    Registro<Cupom> cupons(List<Cupom> cupons) {
        return new Registro<>(cupons);
    }

    @Bean
    Registro<NivelClube> niveisClube(List<NivelClube> niveis) {
        return new Registro<>(niveis);
    }

    @Bean
    Registro<FormaPagamento> formasPagamento(List<FormaPagamento> formas) {
        return new Registro<>(formas);
    }
}
