package com.loja.checkout.service;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CodigoErro;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra.
 *
 * <p>O valor final é montado nesta ordem: produtos, desconto do cupom, frete, seguro,
 * total do pedido (= produtos − desconto + frete + seguro) e, por fim, o ajuste da forma
 * de pagamento sobre esse total. Cada valor em dinheiro é arredondado para centavos em
 * cada etapa (ver {@link Dinheiro}).
 */
@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        // As validações seguem exatamente a ordem combinada: devolve o primeiro problema.
        List<ItemPedido> itens = validarItens(req.itens());
        NivelClube nivel = parse(NivelClube.class, req.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = parse(Regiao.class, req.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = parse(ModalidadeEntrega.class, req.modalidadeEntrega(),
                CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoPedido = pesoTotal(itens);
        if (!modalidade.atende(pesoPedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = Dinheiro.centavos(subtotalProdutos(itens));

        // Frete: zera quando o nível do clube não paga frete.
        BigDecimal frete = nivel.isFreteGratis()
                ? Dinheiro.centavos(BigDecimal.ZERO)
                : modalidade.calcularFrete(pesoPedido);

        // Cupom (opcional): valida existência e aplicabilidade, depois calcula o desconto.
        BigDecimal descontoCupom = Dinheiro.centavos(BigDecimal.ZERO);
        if (temCupom(req.cupom())) {
            Cupom cupom = parse(Cupom.class, req.cupom(), CodigoErro.CUPOM_INVALIDO);
            var ctx = new Cupom.ContextoCupom(subtotal, frete, itens);
            if (!cupom.aplicavel(ctx)) {
                throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.desconto(ctx);
        }

        BigDecimal seguro = regiao.calcularSeguro(subtotal);

        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento = parse(FormaPagamento.class, req.formaPagamento(),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        FormaPagamento.ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido));

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                nivel.calcularCredito(subtotal),
                nivel.ganhaBrinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<CheckoutRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validos = new ArrayList<>(itens.size());
        for (CheckoutRequest.Item item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
            }
            validos.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean temCupom(String cupom) {
        return cupom != null && !cupom.isBlank();
    }

    private static BigDecimal subtotalProdutos(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.total());
        }
        return soma;
    }

    private static BigDecimal pesoTotal(List<ItemPedido> itens) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.peso());
        }
        return soma;
    }

    private static <E extends Enum<E>> E parse(Class<E> tipo, String valor, CodigoErro erro) {
        if (valor == null) {
            throw new CheckoutException(erro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(erro);
        }
    }
}
