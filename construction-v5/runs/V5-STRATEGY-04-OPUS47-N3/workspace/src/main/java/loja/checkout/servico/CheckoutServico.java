package loja.checkout.servico;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

import loja.checkout.dominio.Cupom;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Entrega;
import loja.checkout.dominio.ErroCheckout;
import loja.checkout.dominio.FormaPagamento;
import loja.checkout.dominio.Item;
import loja.checkout.dominio.NivelClube;
import loja.checkout.dominio.Pagamento;
import loja.checkout.dominio.Regiao;
import loja.checkout.web.ItemRequisicao;
import loja.checkout.web.RequisicaoResumo;
import loja.checkout.web.RespostaResumo;

@Service
public class CheckoutServico {

    public RespostaResumo calcular(RequisicaoResumo req) {
        List<Item> itens = itensValidos(req.itens());
        NivelClube nivel = parse(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parse(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        Entrega entrega = parse(Entrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal peso = peso(itens);
        if (!entrega.atende(peso)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = subtotal(itens);
        BigDecimal freteBase = entrega.valorFrete(peso);
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.centavos(BigDecimal.ZERO) : freteBase;

        Cupom cupom = null;
        BigDecimal descontoCupom = Dinheiro.centavos(BigDecimal.ZERO);
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = parse(Cupom.class, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotal)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(subtotal, frete, itens);
        }

        FormaPagamento forma = parse(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (!forma.atende(totalPedido)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Pagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(totalPedido.add(pagamento.ajuste()));

        return new RespostaResumo(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                pagamento.ajuste(),
                totalFinal,
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static List<Item> itensValidos(List<ItemRequisicao> entrada) {
        if (entrada == null || entrada.isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>(entrada.size());
        for (ItemRequisicao i : entrada) {
            if (i == null
                    || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                    || i.quantidade() == null || i.quantidade() <= 0
                    || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            itens.add(new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()));
        }
        return itens;
    }

    private static BigDecimal subtotal(List<Item> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item i : itens) {
            soma = soma.add(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return Dinheiro.centavos(soma);
    }

    private static BigDecimal peso(List<Item> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item i : itens) {
            soma = soma.add(i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return soma;
    }

    private static <E extends Enum<E>> E parse(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new ErroCheckout(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckout(codigoErro);
        }
    }
}
