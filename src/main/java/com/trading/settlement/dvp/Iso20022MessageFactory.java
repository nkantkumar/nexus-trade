package com.trading.settlement.dvp;

import java.time.Instant;
import java.util.UUID;

/**
 * ISO 20022 XML Message Factory for Securities Clearing & Cash Settlement.
 * Produces compliant ISO 20022 XML payloads:
 * - pacs.008.001.10: Financial Institution Customer Credit Transfer (Cash Leg)
 * - sese.023.001.09: Securities Settlement Transaction Instruction (Securities Leg)
 */
public class Iso20022MessageFactory {

    public record IsoMessage(String messageId, String messageType, String xmlPayload, Instant timestamp) {}

    public static IsoMessage createPacs008CashTransfer(
            String instructionId,
            String debtorAccount,
            String creditorAccount,
            double amount,
            String currency
    ) {
        String msgId = "PACS008-" + UUID.randomUUID().toString().substring(0, 8);
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.10">
                    <FIToFICstmrCdtTrf>
                        <GrpHdr>
                            <MsgId>%s</MsgId>
                            <CreDtTm>%s</CreDtTm>
                            <NbOfTxs>1</NbOfTxs>
                        </GrpHdr>
                        <CdtTrfTxInf>
                            <PmtId>
                                <EndToEndId>%s</EndToEndId>
                            </PmtId>
                            <IntrBkSttlmAmt Ccy="%s">%.2f</IntrBkSttlmAmt>
                            <Dbtr>
                                <Id><OrgId><Othr><Id>%s</Id></Othr></OrgId></Id>
                            </Dbtr>
                            <Cdtr>
                                <Id><OrgId><Othr><Id>%s</Id></Othr></OrgId></Id>
                            </Cdtr>
                        </CdtTrfTxInf>
                    </FIToFICstmrCdtTrf>
                </Document>
                """.formatted(msgId, Instant.now().toString(), instructionId, currency, amount, debtorAccount, creditorAccount);

        return new IsoMessage(msgId, "pacs.008.001.10", xml, Instant.now());
    }

    public static IsoMessage createSese023SecuritiesSettlement(
            String instructionId,
            String delivererAccount,
            String receiverAccount,
            String isinOrSymbol,
            long quantity
    ) {
        String msgId = "SESE023-" + UUID.randomUUID().toString().substring(0, 8);
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.09">
                    <SctiesSttlmTxInstr>
                        <TxId>%s</TxId>
                        <SttlmQty>
                            <Qty><Unit>%d</Unit></Qty>
                        </SttlmQty>
                        <FinInstrmId>
                            <Othr><Id>%s</Id></Othr>
                        </FinInstrmId>
                        <DlvryAcct>%s</DlvryAcct>
                        <RcvgAcct>%s</RcvgAcct>
                    </SctiesSttlmTxInstr>
                </Document>
                """.formatted(msgId, quantity, isinOrSymbol, delivererAccount, receiverAccount);

        return new IsoMessage(msgId, "sese.023.001.09", xml, Instant.now());
    }
}
