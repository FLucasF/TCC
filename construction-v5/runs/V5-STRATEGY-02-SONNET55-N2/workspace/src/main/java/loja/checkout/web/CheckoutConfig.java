package loja.checkout.web;

import java.util.List;
import loja.checkout.Catalogo;
import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.ModalidadeEntrega;
import loja.checkout.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckoutConfig {
    @Bean
    Catalogo<ModalidadeEntrega> entregas(List<ModalidadeEntrega> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<Cupom> cupons(List<Cupom> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<NivelClube> niveis(List<NivelClube> opcoes) {
        return new Catalogo<>(opcoes);
    }

    @Bean
    Catalogo<FormaPagamento> pagamentos(List<FormaPagamento> opcoes) {
        return new Catalogo<>(opcoes);
    }
}
