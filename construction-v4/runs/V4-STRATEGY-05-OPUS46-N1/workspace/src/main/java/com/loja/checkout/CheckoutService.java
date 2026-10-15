package com.loja.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        List<ItemCarrinho> itens = validarItens(pedido.itens());
        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        NivelClube nivelClube = parsarEnum(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parsarEnum(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parsarEnum(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isBlank()) {
            cupom = parsarEnum(Cupom.class, pedido.cupom(), "CUPOM_INVALIDO");
            if (!cupom.isAplicavel(itens, subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
        }

        FormaPagamento formaPagamento = parsarEnum(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = nivelClube.isFreteGratis()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(itens, subtotal, frete)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (!formaPagamento.isDisponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        FormaPagamento.ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajustePagamento,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivelClube.calcularCredito(subtotal),
                nivelClube.temBrinde(subtotal)
        );
    }

    private List<ItemCarrinho> validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : itens) {
            if (item == null
                    || item.precoUnitario() == null
                    || item.quantidade() == null
                    || item.pesoKg() == null
                    || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() <= 0
                    || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
        return itens;
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
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
