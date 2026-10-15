package com.loja.checkout.resumo;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.comum.Registros;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.pedido.ItemPedido;
import com.loja.checkout.seguro.Regiao;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o resumo da compra. O que é igual em todo pedido mora aqui: a ordem
 * das etapas do valor final e a ordem das validações. O que muda de caso para
 * caso (entrega, cupom, nível, pagamento, região) foi delegado aos componentes
 * próprios de cada caso, escolhidos por código.
 */
@Service
public class ResumoService {

    private final Registros registros;

    public ResumoService(Registros registros) {
        this.registros = registros;
    }

    public ResumoResponse calcular(PedidoRequest req) {
        // 1. Itens válidos e subtotal dos produtos.
        List<ItemPedido> itens = validarItens(req.itens());
        BigDecimal subtotal = Dinheiro.centavos(somar(itens, ItemPedido::totalLinha));

        // 2. Nível do clube.
        NivelClube nivel = registros.niveis().buscar(req.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));

        // 3. Região (só a porcentagem do seguro muda por região).
        Regiao regiao = parseRegiao(req.regiao());

        // 4. Modalidade de entrega.
        ModalidadeEntrega modalidade = registros.entregas().buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        // 5. A modalidade atende o peso do pedido?
        BigDecimal peso = somar(itens, ItemPedido::pesoLinha);
        if (!modalidade.atende(peso)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Frete: o da modalidade, zerado quando o nível isenta (ex.: OURO).
        BigDecimal frete = nivel.freteGratis()
                ? Dinheiro.centavos(BigDecimal.ZERO)
                : modalidade.frete(peso);

        // 6 e 7. Cupom: desconto (depende do frete para o FRETEGRATIS).
        BigDecimal desconto = descontoCupom(req.cupom(), subtotal, frete, itens);

        // Seguro: porcentagem da região sobre os produtos.
        BigDecimal seguro = regiao.seguro(subtotal);

        // Total do pedido = produtos - desconto + frete + seguro.
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(desconto).add(frete).add(seguro));

        // 8, 9, 10. Forma de pagamento, parcelas e disponibilidade.
        FormaPagamento pagamento = registros.pagamentos().buscar(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Ajuste da forma de pagamento sobre o total.
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = Dinheiro.centavos(resultado.valorFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.valorFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> validos = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validos.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private Regiao parseRegiao(String regiao) {
        if (regiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private BigDecimal descontoCupom(String codigo, BigDecimal subtotal, BigDecimal frete, List<ItemPedido> itens) {
        if (codigo == null) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
        Cupom cupom = registros.cupons().buscar(codigo)
                .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        ContextoCupom ctx = new ContextoCupom(subtotal, frete, itens);
        if (!cupom.aplicavel(ctx)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(ctx);
    }

    private BigDecimal somar(List<ItemPedido> itens, java.util.function.Function<ItemPedido, BigDecimal> parte) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(parte.apply(item));
        }
        return total;
    }
}
