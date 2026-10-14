package br.com.loja.pedidos.checkout.pagamento;

import br.com.loja.pedidos.comum.Registro;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FormasPagamento {

    private final Registro<FormaPagamento> registro;

    public FormasPagamento(List<FormaPagamento> formas) {
        this.registro = new Registro<>(formas, FormaPagamento::codigo);
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return registro.buscar(codigo);
    }
}
