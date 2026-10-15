package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");
    private static final int MAX_PARCELAS_SEM_JUROS = 3;

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        NivelClube nivel = parseEnum(NivelClube.class, request.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, request.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPesoTotal(request.itens());
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = parseEnum(Cupom.class, request.cupom(), "CUPOM_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(request.itens());

        if (cupom != null && !cupom.aplicavel(request.itens(), subtotal)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento pagamento = parseEnum(FormaPagamento.class, request.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!pagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivel.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(request.itens(), subtotal, frete);
        }

        BigDecimal seguro = round(subtotal.multiply(regiao.getTaxaSeguro()));

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (pagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (pagamento) {
            case PIX -> {
                BigDecimal desconto = round(totalPedido.multiply(DESCONTO_PIX));
                totalFinal = totalPedido.subtract(desconto);
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = totalPedido.add(TARIFA_BOLETO);
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
                    totalFinal = totalPedido;
                    valorParcela = round(totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN));
                } else {
                    valorParcela = calcularParcelaPrice(totalPedido, parcelas);
                    totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        BigDecimal credito = round(subtotal.multiply(nivel.getTaxaCredito()));
        boolean brinde = nivel.isElegiBrinde() && subtotal.compareTo(LIMIAR_BRINDE) > 0;

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

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String codigoErro) {
        if (value == null || value.isBlank()) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(codigoErro);
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(round(item.precoUnitario().multiply(new BigDecimal(item.quantidade()))));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal calcularParcelaPrice(BigDecimal total, int numParcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal potencia = BigDecimal.ONE;
        for (int i = 0; i < numParcelas; i++) {
            potencia = potencia.multiply(umMaisTaxa);
        }
        BigDecimal denominador = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN)
        );
        BigDecimal parcela = total.multiply(TAXA_JUROS).divide(denominador, 20, RoundingMode.HALF_EVEN);
        return round(parcela);
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
