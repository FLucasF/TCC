package com.loja.checkout.servico;

import static com.loja.checkout.dominio.CodigoErro.*;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.PedidoRequest;
import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.ErroNegocio;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResumoCompra;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    public ResumoCompra calcular(PedidoRequest req) {
        Pedido pedido = montarPedido(req.itens());
        NivelClube nivel = buscar(NivelClube.values(), req.nivelClube(), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = buscar(Regiao.values(), req.regiao(), REGIAO_INVALIDA);
        Entrega entrega = buscar(Entrega.values(), req.modalidadeEntrega(), MODALIDADE_INVALIDA);
        if (!entrega.atende(pedido.pesoTotal())) {
            throw new ErroNegocio(MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = req.cupom() == null ? null : buscar(Cupom.values(), req.cupom(), CUPOM_INVALIDO);
        if (cupom != null && !cupom.aplicavel(pedido)) {
            throw new ErroNegocio(CUPOM_NAO_APLICAVEL);
        }
        FormaPagamento forma = buscar(FormaPagamento.values(), req.formaPagamento(), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new ErroNegocio(PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.ajustarFrete(entrega.frete(pedido.pesoTotal()));
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = Dinheiro.percentual(subtotal, regiao.taxaSeguro());
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!forma.aceita(total)) {
            throw new ErroNegocio(FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Cobranca cobranca = forma.aplicar(total, parcelas);
        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(total),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(pedido),
                nivel.temBrinde(pedido));
    }

    private static Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroNegocio(PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(ResumoCompraService::montarItem).toList());
    }

    private static ItemPedido montarItem(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || item.quantidade() == null
                || item.quantidade() <= 0 || !positivo(item.pesoKg())) {
            throw new ErroNegocio(PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static <E extends Enum<E>> E buscar(E[] valores, String codigo, CodigoErro erro) {
        return Arrays.stream(valores)
                .filter(valor -> valor.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ErroNegocio(erro));
    }
}
