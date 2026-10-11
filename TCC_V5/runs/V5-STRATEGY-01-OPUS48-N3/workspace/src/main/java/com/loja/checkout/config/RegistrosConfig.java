package com.loja.checkout.config;

import com.loja.checkout.dominio.RegistroPorCodigo;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Monta o registro de busca por código de cada dimensão a partir dos beans
 * registrados. Adicionar um caso novo (uma entrega, um cupom, um nível, uma
 * forma de pagamento) é só criar mais um @Component; ele entra aqui sozinho.
 */
@Configuration
public class RegistrosConfig {

    @Bean
    public RegistroPorCodigo<ModalidadeEntrega> registroEntregas(List<ModalidadeEntrega> entregas) {
        return new RegistroPorCodigo<>(entregas);
    }

    @Bean
    public RegistroPorCodigo<Cupom> registroCupons(List<Cupom> cupons) {
        return new RegistroPorCodigo<>(cupons);
    }

    @Bean
    public RegistroPorCodigo<NivelClube> registroNiveis(List<NivelClube> niveis) {
        return new RegistroPorCodigo<>(niveis);
    }

    @Bean
    public RegistroPorCodigo<FormaPagamento> registroPagamentos(List<FormaPagamento> pagamentos) {
        return new RegistroPorCodigo<>(pagamentos);
    }
}
