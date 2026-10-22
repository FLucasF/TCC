package br.com.maretalta.report;

import br.com.maretalta.store.Store;

/** Um tipo de relatório. Para criar outro, implemente e registre em {@link Reports#all}. */
public interface ReportType {
    /** Nome usado na URL (/reports/{name}) e no título. */
    String name();

    /** Nome do parâmetro de período na URL (date, month, ...). */
    String periodParam();

    /** Valida o período informado e monta o relatório. Lança ApiException(400) se o período for inválido. */
    Report build(Store.Tx tx, String rawPeriod);
}
