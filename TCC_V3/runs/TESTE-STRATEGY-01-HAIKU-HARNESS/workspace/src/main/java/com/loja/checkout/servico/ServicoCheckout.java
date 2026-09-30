package com.loja.checkout.servico;

import com.loja.checkout.dto.ItemDTO;
import com.loja.checkout.dto.RequisicaoCheckoutDTO;
import com.loja.checkout.dto.RespostaCheckoutDTO;
import com.loja.checkout.estrategia.clube.EstrategiaClube;
import com.loja.checkout.estrategia.clube.FabricaClube;
import com.loja.checkout.estrategia.cupom.EstrategiaCupom;
import com.loja.checkout.estrategia.cupom.FabricaCupom;
import com.loja.checkout.estrategia.entrega.EstrategiaEntrega;
import com.loja.checkout.estrategia.entrega.FabricaEntrega;
import com.loja.checkout.estrategia.pagamento.EstrategiaPagamento;
import com.loja.checkout.estrategia.pagamento.FabricaPagamento;
import com.loja.checkout.estrategia.regiao.EstrategiaRegiao;
import com.loja.checkout.estrategia.regiao.FabricaRegiao;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServicoCheckout {

    public RespostaCheckoutDTO calcularResumo(RequisicaoCheckoutDTO requisicao) {
        validarRequisicao(requisicao);

        List<BigDecimal> precos = new ArrayList<>();
        List<Integer> quantidades = new ArrayList<>();
        BigDecimal pesoTotal = BigDecimal.ZERO;
        BigDecimal subtotalProdutos = BigDecimal.ZERO;

        for (ItemDTO item : requisicao.itens) {
            BigDecimal precoItem = item.precoUnitario.multiply(new BigDecimal(item.quantidade));
            subtotalProdutos = subtotalProdutos.add(precoItem);
            precos.add(item.precoUnitario);
            quantidades.add(item.quantidade);
            pesoTotal = pesoTotal.add(item.pesoKg.multiply(new BigDecimal(item.quantidade)));
        }

        subtotalProdutos = Arredondamento.arredondarMeioParaPar(subtotalProdutos);

        EstrategiaCupom cupom = FabricaCupom.criar(requisicao.cupom, precos);
        BigDecimal descontoCupom = BigDecimal.ZERO;
        boolean isCupomFreteGratis = requisicao.cupom != null && requisicao.cupom.equals("FRETEGRATIS");

        if (cupom != null) {
            cupom.validar(subtotalProdutos, quantidades);
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, quantidades);
        }
        descontoCupom = Arredondamento.arredondarMeioParaPar(descontoCupom);

        EstrategiaEntrega entrega = FabricaEntrega.criar(requisicao.modalidadeEntrega);
        entrega.validar(pesoTotal);

        EstrategiaClube clube = FabricaClube.criar(requisicao.nivelClube);
        BigDecimal descontoFrete = BigDecimal.ZERO;
        if (clube.getDescontoFrete().compareTo(BigDecimal.ZERO) > 0) {
            descontoFrete = BigDecimal.ONE;
        }

        BigDecimal frete = isCupomFreteGratis ? descontoCupom : entrega.calcularFrete(pesoTotal);
        if (descontoFrete.compareTo(BigDecimal.ZERO) > 0 && !isCupomFreteGratis) {
            frete = BigDecimal.ZERO;
        }
        frete = Arredondamento.arredondarMeioParaPar(frete);

        BigDecimal baseImposto = subtotalProdutos.subtract(descontoCupom);
        baseImposto = Arredondamento.arredondarMeioParaPar(baseImposto);

        BigDecimal imposto = BigDecimal.ZERO;
        if (requisicao.regiao != null && !requisicao.regiao.isBlank()) {
            EstrategiaRegiao regiao = FabricaRegiao.criar(requisicao.regiao);
            imposto = baseImposto.multiply(regiao.getAliquota());
            imposto = Arredondamento.arredondarMeioParaPar(imposto);
        }

        BigDecimal totalAntesPagamento = subtotalProdutos.subtract(descontoCupom).add(frete).add(imposto);
        totalAntesPagamento = Arredondamento.arredondarMeioParaPar(totalAntesPagamento);

        EstrategiaPagamento pagamento = FabricaPagamento.criar(requisicao.formaPagamento);
        pagamento.validar(requisicao.parcelas, totalAntesPagamento);

        BigDecimal ajustePagamento = pagamento.calcularAjuste(totalAntesPagamento, requisicao.parcelas);
        ajustePagamento = Arredondamento.arredondarMeioParaPar(ajustePagamento);

        BigDecimal totalFinal = totalAntesPagamento.add(ajustePagamento);
        totalFinal = Arredondamento.arredondarMeioParaPar(totalFinal);

        BigDecimal valorParcela = totalFinal.divide(new BigDecimal(requisicao.parcelas), 2, java.math.RoundingMode.HALF_EVEN);

        BigDecimal credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.verificarBrinde(subtotalProdutos);

        return new RespostaCheckoutDTO(
            subtotalProdutos,
            descontoCupom,
            frete,
            entrega.getPrazo(),
            imposto,
            ajustePagamento,
            totalFinal,
            requisicao.parcelas,
            valorParcela,
            credito,
            brinde
        );
    }

    private void validarRequisicao(RequisicaoCheckoutDTO requisicao) {
        if (requisicao.itens == null || requisicao.itens.isEmpty()) {
            throw new IllegalArgumentException("PEDIDO_INVALIDO");
        }

        for (ItemDTO item : requisicao.itens) {
            if (item.precoUnitario == null || item.precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
            if (item.quantidade <= 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
            if (item.pesoKg == null || item.pesoKg.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
        }
    }
}
