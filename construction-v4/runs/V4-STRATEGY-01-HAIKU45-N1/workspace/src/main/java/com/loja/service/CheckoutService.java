package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.CheckoutRequestDto;
import com.loja.dto.CheckoutResponseDto;
import com.loja.dto.ErrorResponseDto;
import com.loja.dto.ItemDto;
import com.loja.service.estrategia.*;
import com.loja.util.Arredondador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckoutService {
    private static final BigDecimal VALOR_BRINDE = new BigDecimal("500.00");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public Object calcularResumo(CheckoutRequestDto request) {
        var erro = validarRequisicao(request);
        if (erro != null) {
            return erro;
        }

        List<Item> itens = request.itens.stream()
                .map(dto -> new Item(dto.nome, dto.precoUnitario, dto.quantidade, dto.pesoKg))
                .collect(Collectors.toList());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
        BigDecimal pesoTotal = calcularPesoTotal(itens);

        NivelClube nivelClube = NivelClube.valueOf(request.nivelClube);
        Regiao regiao = Regiao.valueOf(request.regiao);
        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.formaPagamento);
        ModalidadeEntrega modalidadeEntrega = ModalidadeEntrega.valueOf(request.modalidadeEntrega);

        CalculoEntrega entrega = FabricaEntrega.criar(modalidadeEntrega);
        CalculoCupom cupom = FabricaCupom.criar(request.cupom, itens);

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        Integer prazo = entrega.getPrazo();

        BigDecimal descontoFrete = cupom.calcularDescontoFrete(frete);
        BigDecimal freteAposDesconto = frete.subtract(descontoFrete);

        BigDecimal seguro = CalculoSeguro.calcular(subtotalProdutos, regiao);

        BigDecimal descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete);

        BigDecimal totalAntesAjuste = subtotalProdutos
                .subtract(descontoCupom)
                .add(freteAposDesconto)
                .add(seguro);
        totalAntesAjuste = Arredondador.arredondarParaCentavos(totalAntesAjuste);

        boolean eOuro = nivelClube == NivelClube.OURO;
        if (eOuro) {
            freteAposDesconto = BigDecimal.ZERO;
            totalAntesAjuste = subtotalProdutos
                    .subtract(descontoCupom)
                    .add(seguro);
            totalAntesAjuste = Arredondador.arredondarParaCentavos(totalAntesAjuste);
        }

        erro = validarFormaPagamento(formaPagamento, totalAntesAjuste);
        if (erro != null) {
            return erro;
        }

        Integer parcelas = request.parcelas != null ? request.parcelas : 1;

        erro = validarParcelamento(formaPagamento, parcelas);
        if (erro != null) {
            return erro;
        }

        BigDecimal ajustePagamento;
        BigDecimal totalFinal;
        BigDecimal valorParcela;

        if (formaPagamento == FormaPagamento.CARTAO) {
            valorParcela = calcularParcelaCartao(totalAntesAjuste, parcelas);
            totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            totalFinal = Arredondador.arredondarParaCentavos(totalFinal);
            ajustePagamento = totalFinal.subtract(totalAntesAjuste);
            ajustePagamento = Arredondador.arredondarParaCentavos(ajustePagamento);
        } else if (formaPagamento == FormaPagamento.PIX) {
            ajustePagamento = totalAntesAjuste.multiply(new BigDecimal("0.05"));
            ajustePagamento = Arredondador.arredondarParaCentavos(ajustePagamento);
            ajustePagamento = ajustePagamento.negate();
            totalFinal = totalAntesAjuste.add(ajustePagamento);
            totalFinal = Arredondador.arredondarParaCentavos(totalFinal);
            valorParcela = totalFinal;
        } else {
            ajustePagamento = new BigDecimal("3.49");
            totalFinal = totalAntesAjuste.add(ajustePagamento);
            totalFinal = Arredondador.arredondarParaCentavos(totalFinal);
            valorParcela = totalFinal;
        }

        BigDecimal creditoProximaCompra = CalculoCredito.calcular(subtotalProdutos, nivelClube);

        boolean temBrinde = eOuro && subtotalProdutos.compareTo(VALOR_BRINDE) > 0;

        return new CheckoutResponseDto(
                Arredondador.arredondarParaCentavos(subtotalProdutos),
                Arredondador.arredondarParaCentavos(descontoCupom),
                Arredondador.arredondarParaCentavos(freteAposDesconto),
                prazo,
                Arredondador.arredondarParaCentavos(seguro),
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                creditoProximaCompra,
                temBrinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            subtotal = subtotal.add(item.calcularSubtotal());
        }
        return Arredondador.arredondarParaCentavos(subtotal);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            peso = peso.add(item.pesoKg.multiply(new BigDecimal(item.quantidade)));
        }
        return peso;
    }

    private ErrorResponseDto validarRequisicao(CheckoutRequestDto request) {
        if (request.itens == null || request.itens.isEmpty()) {
            return new ErrorResponseDto("PEDIDO_INVALIDO");
        }

        for (ItemDto item : request.itens) {
            if (item.precoUnitario == null || item.precoUnitario.compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade == null || item.quantidade <= 0 ||
                item.pesoKg == null || item.pesoKg.compareTo(BigDecimal.ZERO) <= 0) {
                return new ErrorResponseDto("PEDIDO_INVALIDO");
            }
        }

        if (request.nivelClube == null || request.nivelClube.isEmpty()) {
            return new ErrorResponseDto("NIVEL_CLUBE_INVALIDO");
        }

        try {
            NivelClube.valueOf(request.nivelClube);
        } catch (IllegalArgumentException e) {
            return new ErrorResponseDto("NIVEL_CLUBE_INVALIDO");
        }

        if (request.regiao == null || request.regiao.isEmpty()) {
            return new ErrorResponseDto("REGIAO_INVALIDA");
        }

        try {
            Regiao.valueOf(request.regiao);
        } catch (IllegalArgumentException e) {
            return new ErrorResponseDto("REGIAO_INVALIDA");
        }

        if (request.modalidadeEntrega == null || request.modalidadeEntrega.isEmpty()) {
            return new ErrorResponseDto("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade;
        try {
            modalidade = ModalidadeEntrega.valueOf(request.modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            return new ErrorResponseDto("MODALIDADE_INVALIDA");
        }

        BigDecimal pesoTotal = calcularPesoTotal(
                request.itens.stream()
                        .map(dto -> new Item(dto.nome, dto.precoUnitario, dto.quantidade, dto.pesoKg))
                        .collect(Collectors.toList())
        );

        CalculoEntrega entrega = FabricaEntrega.criar(modalidade);
        if (!entrega.disponivel(pesoTotal)) {
            return new ErrorResponseDto("MODALIDADE_INDISPONIVEL");
        }

        if (request.cupom != null && !request.cupom.isEmpty()) {
            List<Item> itens = request.itens.stream()
                    .map(dto -> new Item(dto.nome, dto.precoUnitario, dto.quantidade, dto.pesoKg))
                    .collect(Collectors.toList());

            CalculoCupom cupom = FabricaCupom.criar(request.cupom, itens);

            if (cupom == null) {
                return new ErrorResponseDto("CUPOM_INVALIDO");
            }

            BigDecimal subtotalProdutos = calcularSubtotalProdutos(itens);
            if (!cupom.aplicavel(subtotalProdutos)) {
                return new ErrorResponseDto("CUPOM_NAO_APLICAVEL");
            }
        }

        if (request.formaPagamento == null || request.formaPagamento.isEmpty()) {
            return new ErrorResponseDto("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            FormaPagamento.valueOf(request.formaPagamento);
        } catch (IllegalArgumentException e) {
            return new ErrorResponseDto("FORMA_PAGAMENTO_INVALIDA");
        }

        return null;
    }

    private ErrorResponseDto validarFormaPagamento(FormaPagamento forma, BigDecimal total) {
        if (forma == FormaPagamento.BOLETO && total.compareTo(LIMITE_BOLETO) > 0) {
            return new ErrorResponseDto("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return null;
    }

    private ErrorResponseDto validarParcelamento(FormaPagamento forma, Integer parcelas) {
        if (forma == FormaPagamento.PIX || forma == FormaPagamento.BOLETO) {
            if (parcelas != null && parcelas != 1) {
                return new ErrorResponseDto("PARCELAMENTO_INVALIDO");
            }
        } else if (forma == FormaPagamento.CARTAO) {
            if (parcelas == null) {
                parcelas = 1;
            }
            if (parcelas < 1 || parcelas > 12) {
                return new ErrorResponseDto("PARCELAMENTO_INVALIDO");
            }
        }
        return null;
    }

    private BigDecimal calcularParcelaCartao(BigDecimal total, Integer numParcelas) {
        if (numParcelas <= 3) {
            BigDecimal parcela = total.divide(new BigDecimal(numParcelas), 10, java.math.RoundingMode.HALF_EVEN);
            return Arredondador.arredondarParaCentavos(parcela);
        }

        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal expoente = umMaisTaxa.pow(numParcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(expoente, 10, java.math.RoundingMode.HALF_EVEN));
        BigDecimal parcela = total.multiply(taxa).divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);

        return Arredondador.arredondarParaCentavos(parcela);
    }
}
