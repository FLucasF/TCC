package com.loja.checkout.pagamento;

import com.loja.checkout.comum.RegistroPorChave;
import com.loja.checkout.dominio.FormaPagamento;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PagamentoRegistro {

    private final RegistroPorChave<FormaPagamento, Pagamento> registro;

    public PagamentoRegistro(List<Pagamento> formas) {
        this.registro = new RegistroPorChave<>(formas, Pagamento::forma);
    }

    public Optional<Pagamento> buscar(FormaPagamento forma) {
        return registro.buscar(forma);
    }
}
