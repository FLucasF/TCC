package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ResumoService {

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());
        NivelClube nivelClube = resolver(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = resolver(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = resolver(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPeso(pedido.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        Cupom cupom = resolverCupom(pedido.cupom());

        BigDecimal freteCalculado = modalidade.calcularFrete(pesoTotal);
        BigDecimal frete = nivelClube.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteCalculado;

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            if (!cupom.aplicavel(subtotal, pedido.itens())) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotal, frete, pedido.itens());
        }

        BigDecimal seguro = arredondar(subtotal.multiply(regiao.taxaSeguro()));

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento formaPagamento = resolver(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        FormaPagamento.ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        BigDecimal credito = arredondar(subtotal.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.elegivelBrinde() && subtotal.compareTo(new BigDecimal("500")) > 0;

        return new ResumoResponse(
                subtotal, descontoCupom, frete, modalidade.prazo(),
                seguro, ajuste, resultado.totalFinal(),
                parcelas, resultado.valorParcela(),
                credito, brinde);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private <E extends Enum<E>> E resolver(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private Cupom resolverCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
