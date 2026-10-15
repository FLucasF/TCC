package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequisicao;
import br.com.loja.checkout.api.ResumoRequisicao;
import br.com.loja.checkout.calculo.CalculadoraResumo;
import br.com.loja.checkout.clube.CatalogoClube;
import br.com.loja.checkout.clube.NivelBronze;
import br.com.loja.checkout.clube.NivelOuro;
import br.com.loja.checkout.clube.NivelPrata;
import br.com.loja.checkout.cupom.CatalogoCupons;
import br.com.loja.checkout.cupom.CupomBemvindo10;
import br.com.loja.checkout.cupom.CupomFreteGratis;
import br.com.loja.checkout.cupom.CupomLeve3Pague2;
import br.com.loja.checkout.cupom.CupomMenos50;
import br.com.loja.checkout.entrega.CatalogoEntregas;
import br.com.loja.checkout.entrega.EntregaEconomica;
import br.com.loja.checkout.entrega.EntregaExpressa;
import br.com.loja.checkout.entrega.EntregaMotoboy;
import br.com.loja.checkout.entrega.RetiradaLoja;
import br.com.loja.checkout.pagamento.CatalogoPagamentos;
import br.com.loja.checkout.pagamento.PagamentoBoleto;
import br.com.loja.checkout.pagamento.PagamentoCartao;
import br.com.loja.checkout.pagamento.PagamentoPix;
import java.math.BigDecimal;
import java.util.List;

/** Itens e montagem de requisicoes usados pelos testes. */
final class Cenarios {

    static final ItemRequisicao CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    static final ItemRequisicao TENIS = item("Tenis", "249.90", 1, "1.20");
    static final ItemRequisicao FONE = item("Fone", "199.90", 2, "0.25");
    static final ItemRequisicao MEIA = item("Meia", "19.90", 7, "0.10");

    private Cenarios() {
    }

    static ItemRequisicao item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequisicao(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    static CalculadoraResumo calculadora() {
        return new CalculadoraResumo(
                new CatalogoEntregas(List.of(new EntregaEconomica(), new EntregaExpressa(),
                        new RetiradaLoja(), new EntregaMotoboy())),
                new CatalogoCupons(List.of(new CupomBemvindo10(), new CupomMenos50(),
                        new CupomFreteGratis(), new CupomLeve3Pague2())),
                new CatalogoClube(List.of(new NivelBronze(), new NivelPrata(), new NivelOuro())),
                new CatalogoPagamentos(List.of(new PagamentoPix(), new PagamentoCartao(),
                        new PagamentoBoleto())));
    }

    static Requisicao pedido(ItemRequisicao... itens) {
        return new Requisicao(List.of(itens), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    /** Montador de requisicao para deixar os testes curtos. */
    record Requisicao(List<ItemRequisicao> itens, String entrega, String cupom, String pagamento,
                      Integer parcelas, String clube, String regiao) {

        Requisicao entrega(String valor) {
            return new Requisicao(itens, valor, cupom, pagamento, parcelas, clube, regiao);
        }

        Requisicao cupom(String valor) {
            return new Requisicao(itens, entrega, valor, pagamento, parcelas, clube, regiao);
        }

        Requisicao pagamento(String valor, Integer numeroParcelas) {
            return new Requisicao(itens, entrega, cupom, valor, numeroParcelas, clube, regiao);
        }

        Requisicao clube(String valor) {
            return new Requisicao(itens, entrega, cupom, pagamento, parcelas, valor, regiao);
        }

        Requisicao regiao(String valor) {
            return new Requisicao(itens, entrega, cupom, pagamento, parcelas, clube, valor);
        }

        ResumoRequisicao montar() {
            return new ResumoRequisicao(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
        }
    }
}
