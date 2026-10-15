package com.loja.dominio;

import org.springframework.stereotype.Component;

@Component
public class CreditoClube implements EfeitoPagamento {

    @Override
    public void aplicar(Estado estado, Mudancas mudancas, Pedido pedido) {
        Cliente cliente = estado.clientes.get(pedido.cpf());
        cliente.somarCredito(pedido.resumo().creditoProximaCompra());
        mudancas.clientes.add(cliente);
    }

    @Override
    public void reverter(Estado estado, Mudancas mudancas, Pedido pedido) {
        Cliente cliente = estado.clientes.get(pedido.cpf());
        cliente.subtrairCredito(pedido.resumo().creditoProximaCompra());
        mudancas.clientes.add(cliente);
    }
}
