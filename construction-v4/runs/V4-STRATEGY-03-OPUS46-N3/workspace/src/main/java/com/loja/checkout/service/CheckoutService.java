package com.loja.checkout.service;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarItens(pedido.itens());

        NivelClube nivel = parsarEnum(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parsarEnum(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parsarEnum(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = calcularSubtotal(pedido.itens());

        Cupom cupom = parsarCupom(pedido.cupom());
        if (cupom != null && !cupom.aplicavel(subtotal, pedido.itens())) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento pagamento = parsarEnum(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!pagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotal, pedido.itens(), frete);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal, descontoCupom, frete, modalidade.prazoDias(), seguro,
                ajuste, resultado.totalFinal(), parcelas, resultado.valorParcela(),
                credito, brinde);
    }

    private void validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemPedido item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private Cupom parsarCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private <E extends Enum<E>> E parsarEnum(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null || valor.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
