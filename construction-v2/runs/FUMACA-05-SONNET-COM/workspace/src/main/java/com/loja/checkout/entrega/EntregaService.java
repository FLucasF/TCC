package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.erro.CheckoutException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EntregaService {

    private final Map<String, OpcaoEntrega> opcoesPorCodigo;

    public EntregaService(List<OpcaoEntrega> opcoes) {
        this.opcoesPorCodigo = opcoes.stream()
                .collect(Collectors.toMap(OpcaoEntrega::codigo, Function.identity()));
    }

    public EntregaCalculada calcular(String modalidade, Pedido pedido) {
        OpcaoEntrega opcao = opcoesPorCodigo.get(modalidade);
        if (opcao == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        if (!opcao.disponivelPara(pedido)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return new EntregaCalculada(Dinheiro.arredondar(opcao.calcularFrete(pedido)), opcao.prazoDias());
    }
}
