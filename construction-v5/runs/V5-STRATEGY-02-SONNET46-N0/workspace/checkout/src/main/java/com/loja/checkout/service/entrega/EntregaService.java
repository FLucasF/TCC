package com.loja.checkout.service.entrega;

import com.loja.checkout.model.CheckoutException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EntregaService {

    private final Map<String, ModalidadeEntrega> modalidades;

    public EntregaService(List<ModalidadeEntrega> modalidades) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
    }

    public ModalidadeEntrega resolverModalidade(String codigo, double pesoTotalKg) {
        if (codigo == null || !modalidades.containsKey(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega m = modalidades.get(codigo);
        if (!m.aceitaPedido(pesoTotalKg)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return m;
    }
}
