package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoPagamentos {

    @Bean
    public Catalogo<FormaPagamento> catalogoFormasPagamento(List<FormaPagamento> formas) {
        return new Catalogo<>(formas, FormaPagamento::codigo);
    }
}
