package com.loja.checkout.pagamento;

import com.loja.checkout.comum.RegistroDeCodigos;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistroPagamentos extends RegistroDeCodigos<FormaPagamento> {

    public RegistroPagamentos(List<FormaPagamento> formas) {
        super(formas);
    }
}
