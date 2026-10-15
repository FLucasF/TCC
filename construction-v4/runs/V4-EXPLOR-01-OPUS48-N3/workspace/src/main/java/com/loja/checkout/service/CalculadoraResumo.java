package com.loja.checkout.service;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.Resumo;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra. Concentra o que é igual em todo pedido: a ordem do
 * cálculo e a ordem das validações. O que varia de caso para caso é delegado a
 * cada família (entrega, cupom, nível, pagamento) e à região.
 */
@Service
public class CalculadoraResumo {

    public Resumo calcular(ResumoRequest req) {
        List<ItemPedido> itens = validarItens(req.itens());

        NivelClube nivel = NivelClube.resolver(req.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.resolver(req.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = ModalidadeEntrega.resolver(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        BigDecimal peso = somar(itens.stream().map(ItemPedido::totalPeso).toList());
        if (!modalidade.disponivel(peso)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = Dinheiro.centavos(somar(itens.stream().map(ItemPedido::totalPreco).toList()));
        BigDecimal frete = nivel.aplicarFrete(modalidade.frete(peso));
        BigDecimal desconto = resolverDesconto(req.cupom(), new Cupom.Contexto(subtotal, frete, itens));

        FormaPagamento forma = FormaPagamento.resolver(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(desconto).add(frete).add(seguro));
        if (!forma.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        FormaPagamento.Resultado pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(pagamento.valorFinal().subtract(totalPedido));

        return new Resumo(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                pagamento.valorFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> validados = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (!positivo(item.precoUnitario()) || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validados.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validados;
    }

    private BigDecimal resolverDesconto(String codigo, Cupom.Contexto contexto) {
        if (codigo == null) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
        Cupom cupom = Cupom.resolver(codigo)
                .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(contexto);
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
