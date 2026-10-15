package com.loja.checkout.core;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomContexto;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. A ordem do cálculo e a ordem das conferências são
 * iguais em todo pedido; o que muda de caso para caso (entrega, cupom, nível,
 * pagamento) mora cada um na sua classe, escolhido por tabela, nunca por uma
 * cadeia de condições.
 */
@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public CheckoutService(List<ModalidadeEntrega> modalidades,
                           List<Cupom> cupons,
                           List<NivelClube> niveis,
                           List<FormaPagamento> pagamentos) {
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.niveis = indexar(niveis, NivelClube::codigo);
        this.pagamentos = indexar(pagamentos, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> itens, Function<T, String> chave) {
        return itens.stream().collect(Collectors.toUnmodifiableMap(chave, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        // 1. Itens válidos e soma dos produtos.
        List<ItemValidado> itens = validarItens(req.itens());
        BigDecimal subtotal = Dinheiro.centavos(somaProdutos(itens));
        BigDecimal pesoPedido = somaPeso(itens);

        // 2/3. Nível do clube e região.
        NivelClube nivel = exigir(niveis.get(req.nivelClube()), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.porNome(req.regiao())
                .orElseThrow(() -> new ResumoException("REGIAO_INVALIDA"));

        // 4/5. Modalidade de entrega e frete (o nível OURO pode zerar o frete).
        ModalidadeEntrega modalidade = exigir(modalidades.get(req.modalidadeEntrega()), "MODALIDADE_INVALIDA");
        if (!modalidade.disponivel(pesoPedido)) {
            throw new ResumoException("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = nivel.ajustarFrete(Dinheiro.centavos(modalidade.frete(pesoPedido)));

        // 6/7. Cupom, se o cliente usou um.
        BigDecimal descontoCupom = calcularDesconto(req.cupom(), new CupomContexto(subtotal, itens, frete));

        // 4 (seguro) e total do pedido.
        BigDecimal seguro = Dinheiro.centavos(regiao.seguro(subtotal));
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 8/9/10. Forma de pagamento e ajuste sobre o total.
        FormaPagamento pagamento = exigir(pagamentos.get(req.formaPagamento()), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new ResumoException("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new ResumoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                Dinheiro.centavos(nivel.credito(subtotal)),
                nivel.brinde(subtotal));
    }

    private List<ItemValidado> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private ItemValidado validarItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
        return new ItemValidado(item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somaProdutos(List<ItemValidado> itens) {
        return itens.stream().map(ItemValidado::totalItem).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somaPeso(List<ItemValidado> itens) {
        return itens.stream().map(ItemValidado::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDesconto(String codigoCupom, CupomContexto ctx) {
        if (codigoCupom == null) {
            return Dinheiro.zero();
        }
        Cupom cupom = exigir(cupons.get(codigoCupom), "CUPOM_INVALIDO");
        if (!cupom.aplicavel(ctx)) {
            throw new ResumoException("CUPOM_NAO_APLICAVEL");
        }
        return Dinheiro.centavos(cupom.desconto(ctx));
    }

    private <T> T exigir(T valor, String codigoErro) {
        if (valor == null) {
            throw new ResumoException(codigoErro);
        }
        return valor;
    }
}
