package com.loja.checkout.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.Arredondamento;
import com.loja.checkout.domain.cupom.EstrategiaCupom;
import com.loja.checkout.domain.cupom.CupomFreteGratis;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.EstrategiaEntrega;
import com.loja.checkout.domain.entrega.RegistroEntregas;
import com.loja.checkout.domain.pagamento.EstrategiaPagamento;
import com.loja.checkout.domain.pagamento.RegistroPagamentos;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.validation.ValidadorPedido;

@Service
public class CalculadoraResumo {
    private final ValidadorPedido validador = new ValidadorPedido();

    public ResumoResponse calcular(
            List<Item> itens,
            String modalidadeEntregaStr,
            String cupomStr,
            String formaPagamentoStr,
            Integer parcelas,
            String nivelClubeStr,
            String regiao) {

        Optional<String> erro = validador.validar(itens, modalidadeEntregaStr, cupomStr, formaPagamentoStr, parcelas, nivelClubeStr, regiao);
        if (erro.isPresent()) {
            return new ResumoResponse(erro.get());
        }

        NivelClube nivelClube = NivelClube.valueOf(nivelClubeStr);
        Regiao regiaoDominio = Regiao.valueOf(regiao);
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(modalidadeEntregaStr);
        FormaPagamento formaPagamento = FormaPagamento.valueOf(formaPagamentoStr);
        int parcelasEfetivas = parcelas != null ? parcelas : 1;

        BigDecimal subtotal = calcularSubtotal(itens);

        BigDecimal descontoCupom;
        if (cupomStr != null && !cupomStr.isEmpty() && !"FRETEGRATIS".equals(cupomStr)) {
            Optional<EstrategiaCupom> estrategia = RegistroCupons.obter(cupomStr);
            if (estrategia.isPresent() && !estrategia.get().aplicavel(subtotal, itens)) {
                return new ResumoResponse("CUPOM_NAO_APLICAVEL");
            }
        }

        descontoCupom = calcularDescontoCupom(itens, cupomStr, subtotal);
        BigDecimal frete = calcularFrete(itens, modalidade, nivelClube);

        if (frete == null) {
            return new ResumoResponse("MODALIDADE_INDISPONIVEL");
        }

        boolean freteGratisAplicado = cupomStr != null && "FRETEGRATIS".equals(cupomStr);
        if (freteGratisAplicado) {
            descontoCupom = frete;
        }

        BigDecimal seguro = calcularSeguro(subtotal, regiaoDominio);
        BigDecimal totalPedido = subtotal
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);
        totalPedido = Arredondamento.arredondar(totalPedido);

        if (totalPedido.compareTo(new BigDecimal("1000.00")) > 0 && formaPagamento == FormaPagamento.BOLETO) {
            return new ResumoResponse("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        EstrategiaPagamento estrategiaPagamento = RegistroPagamentos.obter(formaPagamento).get();

        if (!estrategiaPagamento.disponivel(totalPedido, parcelasEfetivas)) {
            return new ResumoResponse("PARCELAMENTO_INVALIDO");
        }

        BigDecimal ajustePagamento = estrategiaPagamento.calcularAjuste(totalPedido, parcelasEfetivas);
        BigDecimal totalFinal = Arredondamento.arredondar(totalPedido.add(ajustePagamento));
        BigDecimal valorParcela = Arredondamento.arredondar(totalFinal.divide(new BigDecimal(parcelasEfetivas), 2, java.math.RoundingMode.HALF_EVEN));

        BigDecimal crédito = calcularCredito(subtotal, nivelClube);
        boolean brinde = nivelClube == NivelClube.OURO && subtotal.compareTo(new BigDecimal("500.00")) > 0;

        int prazoEntrega = RegistroEntregas.obter(modalidade).get().prazoEntregaDias();

        ResumoResponse resposta = new ResumoResponse();
        resposta.setSubtotalProdutos(subtotal);
        resposta.setDescontoCupom(descontoCupom);
        resposta.setFrete(frete);
        resposta.setPrazoEntregaDias(prazoEntrega);
        resposta.setSeguro(seguro);
        resposta.setAjustePagamento(ajustePagamento);
        resposta.setTotalFinal(totalFinal);
        resposta.setParcelas(parcelasEfetivas);
        resposta.setValorParcela(valorParcela);
        resposta.setCreditoProximaCompra(crédito);
        resposta.setBrinde(brinde);

        return resposta;
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            subtotal = subtotal.add(item.getSubtotalItem());
        }
        return Arredondamento.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(List<Item> itens, String cupomStr, BigDecimal subtotal) {
        if (cupomStr == null || cupomStr.isEmpty() || "FRETEGRATIS".equals(cupomStr)) {
            return BigDecimal.ZERO;
        }

        Optional<EstrategiaCupom> estrategia = RegistroCupons.obter(cupomStr);
        if (estrategia.isPresent()) {
            return estrategia.get().calcularDesconto(subtotal, itens);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularFrete(List<Item> itens, ModalidadeEntrega modalidade, NivelClube nivelClube) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (Item item : itens) {
            pesoTotal = pesoTotal.add(item.getPesoTotal());
        }

        if (nivelClube.isFretGratis()) {
            return BigDecimal.ZERO;
        }

        Optional<EstrategiaEntrega> estrategia = RegistroEntregas.obter(modalidade);
        if (estrategia.isPresent()) {
            EstrategiaEntrega entrega = estrategia.get();
            if (!entrega.disponivel(pesoTotal)) {
                return null;
            }
            return entrega.calcularFrete(pesoTotal);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotal, Regiao regiao) {
        return Arredondamento.arredondar(subtotal.multiply(regiao.getTaxaSeguro()));
    }

    private BigDecimal calcularCredito(BigDecimal subtotal, NivelClube nivelClube) {
        return Arredondamento.arredondar(subtotal.multiply(nivelClube.getCreditoPorcentagem()));
    }
}
