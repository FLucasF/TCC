package com.loja.checkout.pagamento;

import com.loja.checkout.domain.Registro;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PagamentoRegistro extends Registro<FormaPagamento> {

    public PagamentoRegistro(List<FormaPagamento> formas) {
        super(formas, FormaPagamento::codigo);
    }
}
