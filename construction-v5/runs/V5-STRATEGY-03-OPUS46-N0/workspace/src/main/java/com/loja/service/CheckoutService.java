package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.exception.CheckoutException;
import com.loja.model.*;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000");

    public CheckoutResponse calcular(CheckoutRequest request) {
        List<ItemRequest> itens = validarItens(request.itens());
        NivelClube nivelClube = parseEnum(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, request.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        BigDecimal subtotal = calcularSubtotal(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        if (!modalidade.isDisponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = validarCupom(request.cupom(), subtotal);

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        if (!formaPagamento.isParcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivelClube.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, itens, frete)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = totalPedido.multiply(DESCONTO_PIX).setScale(2, RoundingMode.HALF_EVEN);
                totalFinal = totalPedido.subtract(desconto);
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = totalPedido.add(TARIFA_BOLETO);
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= 3) {
                    totalFinal = totalPedido;
                    valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                } else {
                    valorParcela = calcularParcelaPrice(totalPedido, parcelas);
                    totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal ajuste = totalFinal.subtract(totalPedido).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal credito = nivelClube.calcularCredito(subtotal);
        boolean brinde = nivelClube.ganharBrinde(subtotal);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajuste,
                totalFinal,
                parcelas,
                valorParcela,
                credito,
                brinde
        );
    }

    private List<ItemRequest> validarItens(List<ItemRequest> itens) {
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
        return itens;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private Cupom validarCupom(String codigo, BigDecimal subtotal) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        Cupom cupom = parseEnum(Cupom.class, codigo, "CUPOM_INVALIDO");
        if (!cupom.isAplicavel(subtotal)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private BigDecimal calcularParcelaPrice(BigDecimal total, int n) {
        BigDecimal base = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal baseN = base.pow(n);
        BigDecimal inversa = BigDecimal.ONE.divide(baseN, 20, RoundingMode.HALF_EVEN);
        BigDecimal denominador = BigDecimal.ONE.subtract(inversa);
        return total.multiply(TAXA_JUROS).divide(denominador, 2, RoundingMode.HALF_EVEN);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String codigoErro) {
        if (value == null || value.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }
}
