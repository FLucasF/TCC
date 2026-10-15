package com.loja.checkout.service;

import com.loja.checkout.clube.BeneficiosClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.CalculadoraFrete;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    private final Map<String, CalculadoraFrete> fretes;
    private final Map<String, Cupom> cupons;
    private final Map<String, BeneficiosClube> clubes;

    public CheckoutService(List<CalculadoraFrete> calculadorasFretes,
                           List<Cupom> listaCupons,
                           List<BeneficiosClube> listaClubes) {
        this.fretes = calculadorasFretes.stream()
                .collect(Collectors.toMap(CalculadoraFrete::codigo, Function.identity()));
        this.cupons = listaCupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
        this.clubes = listaClubes.stream()
                .collect(Collectors.toMap(BeneficiosClube::codigo, Function.identity()));
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        validarItens(request.itens());

        BeneficiosClube clube = resolverClube(request.nivelClube());
        Regiao regiao = resolverRegiao(request.regiao());
        CalculadoraFrete calculadoraFrete = resolverModalidade(request.modalidadeEntrega());

        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        verificarDisponibilidadeModalidade(calculadoraFrete, pesoTotal);

        Cupom cupom = resolverCupom(request.cupom(), subtotal, request.itens());
        FormaPagamento formaPagamento = resolverFormaPagamento(request.formaPagamento());
        int parcelas = resolverParcelas(request.parcelas(), formaPagamento);

        BigDecimal frete = clube.freteGratis()
                ? BigDecimal.ZERO.setScale(SCALE, ROUNDING)
                : calculadoraFrete.calcularFrete(pesoTotal).setScale(SCALE, ROUNDING);

        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotal, frete, request.itens())
                : BigDecimal.ZERO.setScale(SCALE, ROUNDING);

        BigDecimal seguro = subtotal.multiply(regiao.getTaxaSeguro()).setScale(SCALE, ROUNDING);

        BigDecimal totalPedido = subtotal
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(SCALE, ROUNDING);

        verificarDisponibilidadePagamento(formaPagamento, totalPedido);

        BigDecimal totalFinal;
        BigDecimal valorParcela;

        switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = totalPedido.multiply(DESCONTO_PIX).setScale(SCALE, ROUNDING);
                totalFinal = totalPedido.subtract(desconto).setScale(SCALE, ROUNDING);
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = totalPedido.add(TARIFA_BOLETO).setScale(SCALE, ROUNDING);
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= PARCELAS_SEM_JUROS) {
                    totalFinal = totalPedido;
                    valorParcela = totalPedido.divide(new BigDecimal(parcelas), SCALE, ROUNDING);
                } else {
                    BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_MENSAL);
                    BigDecimal potencia = umMaisTaxa.pow(parcelas, MathContext.DECIMAL128);
                    BigDecimal denominador = BigDecimal.ONE.subtract(
                            BigDecimal.ONE.divide(potencia, MathContext.DECIMAL128));
                    valorParcela = totalPedido.multiply(TAXA_JUROS_MENSAL)
                            .divide(denominador, SCALE, ROUNDING);
                    totalFinal = valorParcela.multiply(new BigDecimal(parcelas)).setScale(SCALE, ROUNDING);
                }
            }
            default -> throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido).setScale(SCALE, ROUNDING);

        BigDecimal creditoProximaCompra = subtotal.multiply(clube.percentualCredito())
                .setScale(SCALE, ROUNDING);

        boolean brinde = clube.brinde(subtotal);

        return new ResumoResponse(
                subtotal.setScale(SCALE, ROUNDING),
                descontoCupom,
                frete,
                calculadoraFrete.prazoDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                creditoProximaCompra,
                brinde
        );
    }

    private void validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BeneficiosClube resolverClube(String nivelClube) {
        if (nivelClube == null || !clubes.containsKey(nivelClube)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return clubes.get(nivelClube);
    }

    private Regiao resolverRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private CalculadoraFrete resolverModalidade(String modalidade) {
        if (modalidade == null || !fretes.containsKey(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return fretes.get(modalidade);
    }

    private void verificarDisponibilidadeModalidade(CalculadoraFrete calculadora, BigDecimal pesoTotal) {
        if (!calculadora.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom resolverCupom(String codigoCupom, BigDecimal subtotal, List<ItemCarrinho> itens) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }
        if (!cupons.containsKey(codigoCupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        Cupom cupom = cupons.get(codigoCupom);
        if (!cupom.aplicavel(subtotal, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private FormaPagamento resolverFormaPagamento(String formaPagamento) {
        if (formaPagamento == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private int resolverParcelas(Integer parcelas, FormaPagamento formaPagamento) {
        int qtd = parcelas != null ? parcelas : 1;
        switch (formaPagamento) {
            case PIX, BOLETO -> {
                if (qtd != 1) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
            case CARTAO -> {
                if (qtd < 1 || qtd > PARCELAS_MAXIMAS) {
                    throw new CheckoutException("PARCELAMENTO_INVALIDO");
                }
            }
        }
        return qtd;
    }

    private void verificarDisponibilidadePagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (formaPagamento == FormaPagamento.BOLETO && totalPedido.compareTo(LIMITE_BOLETO) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(new BigDecimal(item.quantidade()))
            );
        }
        return subtotal.setScale(SCALE, ROUNDING);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }
}
