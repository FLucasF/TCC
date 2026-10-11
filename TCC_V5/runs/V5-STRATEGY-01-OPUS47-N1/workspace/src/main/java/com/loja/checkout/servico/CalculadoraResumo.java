package com.loja.checkout.servico;

import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResposta;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CalculadoraResumo {

    public ResultadoCheckout calcular(CheckoutRequest req) {
        List<Item> itens = extrairItens(req);
        if (itens == null) return falha(CheckoutErro.PEDIDO_INVALIDO);

        NivelClube nivel = parseEnum(NivelClube.class, req.nivelClube);
        if (nivel == null) return falha(CheckoutErro.NIVEL_CLUBE_INVALIDO);

        Regiao regiao = parseEnum(Regiao.class, req.regiao);
        if (regiao == null) return falha(CheckoutErro.REGIAO_INVALIDA);

        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, req.modalidadeEntrega);
        if (modalidade == null) return falha(CheckoutErro.MODALIDADE_INVALIDA);

        BigDecimal subtotal = Dinheiro.arredondar(somaSubtotal(itens));
        BigDecimal pesoKg = somaPeso(itens);

        if (!modalidade.atende(pesoKg)) return falha(CheckoutErro.MODALIDADE_INDISPONIVEL);

        BigDecimal freteBase = modalidade.calcularFrete(pesoKg);
        BigDecimal frete = nivel.ajustarFrete(freteBase);

        Optional<Cupom> cupom;
        if (req.cupom == null || req.cupom.isEmpty()) {
            cupom = Optional.empty();
        } else {
            Cupom c = parseEnum(Cupom.class, req.cupom);
            if (c == null) return falha(CheckoutErro.CUPOM_INVALIDO);
            if (!c.aplicavel(itens, subtotal, frete)) return falha(CheckoutErro.CUPOM_NAO_APLICAVEL);
            cupom = Optional.of(c);
        }

        BigDecimal descontoCupom = cupom
            .map(c -> c.calcularDesconto(itens, subtotal, frete))
            .orElse(Dinheiro.arredondar(BigDecimal.ZERO));

        BigDecimal seguro = regiao.calcularSeguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento forma = parseEnum(FormaPagamento.class, req.formaPagamento);
        if (forma == null) return falha(CheckoutErro.FORMA_PAGAMENTO_INVALIDA);

        int parcelas = req.parcelas == null ? 1 : req.parcelas;
        if (!forma.parcelasValidas(parcelas)) return falha(CheckoutErro.PARCELAMENTO_INVALIDO);
        if (!forma.atende(totalPedido)) return falha(CheckoutErro.FORMA_PAGAMENTO_INDISPONIVEL);

        BigDecimal ajuste = forma.ajuste(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(ajuste));
        BigDecimal valorParcela = forma.valorParcela(totalFinal, parcelas);

        CheckoutResposta resposta = new CheckoutResposta(
            subtotal,
            descontoCupom,
            frete,
            modalidade.prazoDias(),
            seguro,
            Dinheiro.arredondar(ajuste),
            totalFinal,
            parcelas,
            valorParcela,
            nivel.creditoProximaCompra(subtotal),
            nivel.ganhaBrinde(subtotal)
        );
        return new ResultadoCheckout.Sucesso(resposta);
    }

    private List<Item> extrairItens(CheckoutRequest req) {
        if (req.itens == null || req.itens.isEmpty()) return null;
        List<Item> itens = new ArrayList<>(req.itens.size());
        for (CheckoutRequest.ItemRequest i : req.itens) {
            if (i == null) return null;
            if (i.precoUnitario == null || i.precoUnitario.signum() <= 0) return null;
            if (i.quantidade == null || i.quantidade <= 0) return null;
            if (i.pesoKg == null || i.pesoKg.signum() <= 0) return null;
            itens.add(new Item(i.nome, i.precoUnitario, i.quantidade, i.pesoKg));
        }
        return itens;
    }

    private BigDecimal somaSubtotal(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : itens) {
            total = total.add(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return total;
    }

    private BigDecimal somaPeso(List<Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : itens) {
            total = total.add(i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return total;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor) {
        if (valor == null) return null;
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ResultadoCheckout falha(CheckoutErro codigo) {
        return new ResultadoCheckout.Falha(codigo);
    }
}
