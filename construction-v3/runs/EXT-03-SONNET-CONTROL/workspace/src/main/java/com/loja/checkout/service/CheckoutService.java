package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal LIMITE_MENOS50 = new BigDecimal("300.00");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_JUROS_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    public ResumoResponse calcularResumo(ResumoRequest request) {
        List<ItemRequest> itens = request.getItens();
        validarItens(itens);

        NivelClube nivel = parseEnum(NivelClube.class, request.getNivelClube());
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        Regiao regiao = parseEnum(Regiao.class, request.getRegiao());
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.getModalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        String cupomStr = request.getCupom();
        if (cupomStr != null && !cupomStr.isBlank()) {
            cupom = parseEnum(Cupom.class, cupomStr);
            if (cupom == null) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
        }

        BigDecimal subtotal = arredondar(calcularSubtotal(itens));
        BigDecimal freteBase = arredondar(modalidade.calcularFrete(pesoTotal));
        BigDecimal frete = nivel.isFreteGratis() ? BigDecimal.ZERO : freteBase;

        BigDecimal descontoCupom = BigDecimal.ZERO;
        if (cupom != null) {
            if (!cupom.aplicavel(itens, subtotal, frete)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = arredondar(cupom.calcularDesconto(itens, subtotal, frete));
        }

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, request.getFormaPagamento());
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        int parcelas = request.getParcelas() == null ? 1 : request.getParcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal totalSemImposto = subtotal.subtract(descontoCupom).add(frete);
        if (formaPagamento == FormaPagamento.BOLETO && totalSemImposto.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal imposto = arredondar(regiao.getAliquota().multiply(subtotal.subtract(descontoCupom)));
        BigDecimal totalPedido = totalSemImposto.add(imposto);

        BigDecimal totalFinal;
        BigDecimal valorParcela;
        switch (formaPagamento) {
            case PIX -> {
                BigDecimal descontoPix = arredondar(totalPedido.multiply(DESCONTO_PIX));
                totalFinal = totalPedido.subtract(descontoPix);
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = totalPedido.add(TARIFA_BOLETO);
                valorParcela = totalFinal;
            }
            default -> {
                if (parcelas <= PARCELAS_SEM_JUROS) {
                    totalFinal = totalPedido;
                    valorParcela = arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                } else {
                    valorParcela = arredondar(calcularParcelaComJuros(totalPedido, parcelas));
                    totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                }
            }
        }

        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);
        BigDecimal credito = arredondar(subtotal.multiply(nivel.getPercentualCredito()));
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                imposto,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                credito,
                brinde
        );
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_CARTAO);
        BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
        BigDecimal fatorDesconto = BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128);
        BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
        return totalPedido.multiply(TAXA_JUROS_CARTAO).divide(denominador, MathContext.DECIMAL128);
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }
        return subtotal;
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }
        return peso;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
