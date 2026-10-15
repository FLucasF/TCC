package br.com.loja.checkout.servico;

import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoRequest.ItemRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.dominio.Codigos;
import br.com.loja.checkout.dominio.Cupom;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.FormaPagamento;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.ModalidadeEntrega;
import br.com.loja.checkout.dominio.NivelClube;
import br.com.loja.checkout.dominio.Pagamento;
import br.com.loja.checkout.dominio.Regiao;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<Item> itens = itens(pedido.itens());
        NivelClube nivel = Codigos.obrigatorio(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = Codigos.obrigatorio(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = Codigos.obrigatorio(
                ModalidadeEntrega.class, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal peso = pesoTotal(itens);
        if (!modalidade.disponivelPara(peso)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = subtotal(itens);
        Cupom cupom = cupom(pedido.cupom());
        if (cupom != null && !cupom.aplicavel(subtotal)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        FormaPagamento formaPagamento = Codigos.obrigatorio(
                FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!formaPagamento.parcelamentoPermitido(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal frete = nivel.freteGratis()
                ? Dinheiro.ZERO
                : Dinheiro.arredonda(modalidade.calcularFrete(peso));
        BigDecimal desconto = cupom == null
                ? Dinheiro.ZERO
                : Dinheiro.arredonda(cupom.desconto(itens, subtotal, frete));
        BigDecimal seguro = Dinheiro.arredonda(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Pagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = pagamento.totalFinal().subtract(totalPedido);
        BigDecimal credito = Dinheiro.arredonda(subtotal.multiply(nivel.percentualCredito()));

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                credito,
                nivel.temBrinde(subtotal));
    }

    private static List<Item> itens(List<ItemRequest> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return pedidos.stream().map(CalculadoraResumo::item).toList();
    }

    private static Item item(ItemRequest pedido) {
        if (pedido == null
                || !positivo(pedido.precoUnitario())
                || pedido.quantidade() == null || pedido.quantidade() <= 0
                || !positivo(pedido.pesoKg())) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(pedido.precoUnitario(), pedido.quantidade(), pedido.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static BigDecimal pesoTotal(List<Item> itens) {
        return itens.stream()
                .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal subtotal(List<Item> itens) {
        return itens.stream()
                .map(item -> Dinheiro.arredonda(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))))
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private static Cupom cupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return Codigos.obrigatorio(Cupom.class, codigo, CodigoErro.CUPOM_INVALIDO);
    }
}
