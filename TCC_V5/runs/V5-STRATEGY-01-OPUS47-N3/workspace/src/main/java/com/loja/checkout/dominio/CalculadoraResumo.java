package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dominio.regiao.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CalculadoraResumo {

    public CheckoutResponse calcular(CheckoutRequest req) {
        List<Item> itens = validaItens(req.itens());
        NivelClube nivel = parse(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parse(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modal = parse(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal subtotal = Dinheiro.aCentavos(somaProdutos(itens));
        BigDecimal peso = somaPeso(itens);

        if (!modal.disponivelPara(peso)) throw new ErroPedido("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = parse(Cupom.class, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotal)) throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento forma = parse(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValidas(parcelas)) throw new ErroPedido("PARCELAMENTO_INVALIDO");

        BigDecimal freteBruto = modal.custoBruto(peso);
        BigDecimal frete = nivel.ajustaFrete(freteBruto);
        BigDecimal descontoCupom = cupom == null
                ? Dinheiro.ZERO
                : cupom.desconto(subtotal, itens, frete);
        BigDecimal seguro = Dinheiro.aCentavos(subtotal.multiply(regiao.percentual()));
        BigDecimal totalPedido = Dinheiro.aCentavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (!forma.disponivelPara(totalPedido)) throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento pag = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.aCentavos(pag.totalFinal().subtract(totalPedido));

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modal.prazoDias(),
                seguro,
                ajuste,
                pag.totalFinal(),
                parcelas,
                pag.valorParcela(),
                nivel.cashback(subtotal),
                nivel.brinde(subtotal)
        );
    }

    private List<Item> validaItens(List<CheckoutRequest.ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) throw new ErroPedido("PEDIDO_INVALIDO");
        List<Item> resultado = new ArrayList<>(itens.size());
        for (CheckoutRequest.ItemRequest it : itens) {
            if (it == null
                    || it.precoUnitario() == null
                    || it.quantidade() == null
                    || it.pesoKg() == null
                    || it.precoUnitario().signum() <= 0
                    || it.quantidade() <= 0
                    || it.pesoKg().signum() <= 0) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
            resultado.add(new Item(it.nome(), it.precoUnitario(), it.quantidade(), it.pesoKg()));
        }
        return resultado;
    }

    private BigDecimal somaProdutos(List<Item> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item it : itens) soma = soma.add(it.precoUnitario().multiply(BigDecimal.valueOf(it.quantidade())));
        return soma;
    }

    private BigDecimal somaPeso(List<Item> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item it : itens) soma = soma.add(it.pesoKg().multiply(BigDecimal.valueOf(it.quantidade())));
        return soma;
    }

    private <E extends Enum<E>> E parse(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null) throw new ErroPedido(codigoErro);
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new ErroPedido(codigoErro);
        }
    }
}
