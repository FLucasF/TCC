package com.loja.checkout.validation;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.PedidoRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Arrays;

@Service
public class ValidadorPedido {
    public String validar(PedidoRequest request) {
        // 1. Validar itens
        if (request.itens() == null || request.itens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }
        for (var item : request.itens()) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null) {
                return "PEDIDO_INVALIDO";
            }
            BigDecimal preco = new BigDecimal(item.precoUnitario().toString());
            BigDecimal peso = new BigDecimal(item.pesoKg().toString());
            if (preco.compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() <= 0 ||
                peso.compareTo(BigDecimal.ZERO) < 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        // 2. Validar nível do clube
        if (request.nivelClube() == null) {
            return "NIVEL_CLUBE_INVALIDO";
        }
        try {
            NivelClube.valueOf(request.nivelClube());
        } catch (IllegalArgumentException e) {
            return "NIVEL_CLUBE_INVALIDO";
        }

        // 3. Validar região
        if (request.regiao() == null) {
            return "REGIAO_INVALIDA";
        }
        try {
            Regiao.valueOf(request.regiao());
        } catch (IllegalArgumentException e) {
            return "REGIAO_INVALIDA";
        }

        // 4. Validar modalidade de entrega
        if (request.modalidadeEntrega() == null) {
            return "MODALIDADE_INVALIDA";
        }
        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(request.modalidadeEntrega());
        } catch (IllegalArgumentException e) {
            return "MODALIDADE_INVALIDA";
        }

        // 5. Validar se modalidade atende o pedido
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (var item : request.itens()) {
            BigDecimal peso = new BigDecimal(item.pesoKg().toString());
            BigDecimal quantidade = new BigDecimal(item.quantidade());
            pesoTotal = pesoTotal.add(peso.multiply(quantidade));
        }
        if (!modalidade.isDisponivelPara(pesoTotal)) {
            return "MODALIDADE_INDISPONIVEL";
        }

        // 6. Validar cupom
        BigDecimal subtotal = calcularSubtotal(request.itens());
        java.util.List<ItemPedido> itens = converterItens(request.itens());

        if (request.cupom() != null) {
            Cupom cupom = Cupom.porCodigo(request.cupom());
            if (cupom == null) {
                return "CUPOM_INVALIDO";
            }

            // 7. Validar se cupom é aplicável
            if (!cupom.ehValido(subtotal, itens)) {
                return "CUPOM_NAO_APLICAVEL";
            }
        }

        // 8. Validar forma de pagamento
        if (request.formaPagamento() == null) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }
        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(request.formaPagamento());
        } catch (IllegalArgumentException e) {
            return "FORMA_PAGAMENTO_INVALIDA";
        }

        // 9. Validar número de parcelas
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.isParcelavelEm(parcelas)) {
            return "PARCELAMENTO_INVALIDO";
        }

        // 10. Validar se forma de pagamento atende o pedido (boleto acima de 1000)
        if (!formaPagamento.ehDisponivelPara(subtotal)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }

    private BigDecimal calcularSubtotal(java.util.List<PedidoRequest.Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : itens) {
            BigDecimal preco = new BigDecimal(item.precoUnitario().toString());
            BigDecimal quantidade = new BigDecimal(item.quantidade());
            subtotal = subtotal.add(preco.multiply(quantidade));
        }
        return subtotal;
    }

    private java.util.List<ItemPedido> converterItens(java.util.List<PedidoRequest.Item> itens) {
        return itens.stream()
            .map(item -> new ItemPedido(
                item.nome(),
                new BigDecimal(item.precoUnitario().toString()),
                item.quantidade(),
                new BigDecimal(item.pesoKg().toString())
            ))
            .toList();
    }
}
