package com.loja.checkout.service;

import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Pedido;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.service.clube.NivelClube;
import com.loja.checkout.service.cupom.Cupom;
import com.loja.checkout.service.entrega.Modalidade;
import com.loja.checkout.service.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ValidadorPedido {

    public void validarItens(PedidoRequest request) {
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }

        for (var item : request.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
        }
    }

    public Regiao validarRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }

        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    public NivelClube validarNivelClube(String codigo, List<NivelClube> niveis) {
        if (codigo == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        return niveis.stream()
            .filter(n -> n.getCodigo().equals(codigo))
            .findFirst()
            .orElseThrow(() -> new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO));
    }

    public Modalidade validarModalidade(String codigo, Pedido pedido, List<Modalidade> modalidades) {
        if (codigo == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }

        Modalidade modalidade = modalidades.stream()
            .filter(m -> m.getCodigo().equals(codigo))
            .findFirst()
            .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));

        if (!modalidade.aceita(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        return modalidade;
    }

    public Cupom validarCupom(String codigo, BigDecimal subtotal, BigDecimal frete, List<Cupom> cupons) {
        if (codigo == null) {
            return null;
        }

        Cupom cupom = cupons.stream()
            .filter(c -> c.getCodigo().equals(codigo))
            .findFirst()
            .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));

        if (!cupom.aplicavel(subtotal, frete)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        return cupom;
    }

    public FormaPagamento validarFormaPagamento(String codigo, int parcelas, BigDecimal totalPedido,
                                                  List<FormaPagamento> formas) {
        if (codigo == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        FormaPagamento forma = formas.stream()
            .filter(f -> f.getCodigo().equals(codigo))
            .findFirst()
            .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        if (!forma.aceitaParcelas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        if (!forma.aceita(totalPedido, parcelas)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        return forma;
    }

    public Pedido converterParaPedido(PedidoRequest request, Regiao regiao) {
        List<Item> itens = request.itens().stream()
            .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
            .toList();

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        return new Pedido(itens, request.cupom(), parcelas, regiao);
    }
}
