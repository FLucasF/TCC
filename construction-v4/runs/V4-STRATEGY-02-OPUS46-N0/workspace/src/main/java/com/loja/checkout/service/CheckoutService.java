package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.clube.BeneficioClube;
import com.loja.checkout.service.cupom.ProcessadorCupom;
import com.loja.checkout.service.entrega.CalculadoraFrete;
import com.loja.checkout.service.pagamento.ProcessadorPagamento;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private static final Map<String, BigDecimal> TAXA_SEGURO = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private final Map<String, CalculadoraFrete> fretes;
    private final Map<String, ProcessadorCupom> cupons;
    private final Map<String, BeneficioClube> clubes;
    private final Map<String, ProcessadorPagamento> pagamentos;

    public CheckoutService(List<CalculadoraFrete> fretes,
                           List<ProcessadorCupom> cupons,
                           List<BeneficioClube> clubes,
                           List<ProcessadorPagamento> pagamentos) {
        this.fretes = fretes.stream().collect(Collectors.toMap(CalculadoraFrete::codigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(ProcessadorCupom::codigo, Function.identity()));
        this.clubes = clubes.stream().collect(Collectors.toMap(BeneficioClube::nivel, Function.identity()));
        this.pagamentos = pagamentos.stream().collect(Collectors.toMap(ProcessadorPagamento::codigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        validarItens(request.itens());

        BigDecimal subtotal = calcularSubtotal(request.itens());
        BigDecimal pesoTotal = calcularPeso(request.itens());

        BeneficioClube clube = encontrarClube(request.nivelClube());
        BigDecimal taxaSeguro = encontrarTaxaSeguro(request.regiao());
        CalculadoraFrete freteCalc = encontrarFrete(request.modalidadeEntrega(), pesoTotal);

        BigDecimal valorFrete = freteCalc.calcularFrete(pesoTotal);
        int prazo = freteCalc.prazoEntregaDias();
        if (clube.freteGratis()) {
            valorFrete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = calcularDescontoCupom(request.cupom(), subtotal, request.itens(), valorFrete);
        BigDecimal seguro = Moeda.arredondar(subtotal.multiply(taxaSeguro));
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(valorFrete).add(seguro);

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        ProcessadorPagamento pagamento = encontrarPagamento(request.formaPagamento(), parcelas, totalPedido);
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal ajuste = Moeda.arredondar(resultado.totalFinal().subtract(totalPedido));
        BigDecimal credito = clube.creditoProximaCompra(subtotal);
        boolean brinde = clube.brinde(subtotal);

        return new CheckoutResponse(
                subtotal, descontoCupom, valorFrete, prazo, seguro,
                ajuste, resultado.totalFinal(), parcelas, resultado.valorParcela(),
                credito, brinde
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

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(new BigDecimal(item.quantidade())));
        }
        return Moeda.arredondar(subtotal);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(new BigDecimal(item.quantidade())));
        }
        return peso;
    }

    private BeneficioClube encontrarClube(String nivel) {
        if (nivel == null || !clubes.containsKey(nivel)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return clubes.get(nivel);
    }

    private BigDecimal encontrarTaxaSeguro(String regiao) {
        if (regiao == null || !TAXA_SEGURO.containsKey(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        return TAXA_SEGURO.get(regiao);
    }

    private CalculadoraFrete encontrarFrete(String modalidade, BigDecimal pesoTotal) {
        if (modalidade == null || !fretes.containsKey(modalidade)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        CalculadoraFrete frete = fretes.get(modalidade);
        if (!frete.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return frete;
    }

    private BigDecimal calcularDescontoCupom(String cupom, BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        if (cupom == null || cupom.isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (!cupons.containsKey(cupom)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        ProcessadorCupom processador = cupons.get(cupom);
        if (!processador.aplicavel(subtotal, itens)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return processador.calcularDesconto(subtotal, itens, frete);
    }

    private ProcessadorPagamento encontrarPagamento(String forma, int parcelas, BigDecimal totalPedido) {
        if (forma == null || !pagamentos.containsKey(forma)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        ProcessadorPagamento pagamento = pagamentos.get(forma);
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return pagamento;
    }
}
