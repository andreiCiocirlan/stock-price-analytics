package com.example.stockprices.model.quarterly;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.util.List;


@JsonIgnoreType
public class Quarterly {

    @JsonProperty("pfcfTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PfcfTTMQItem> pfcfTTMQ;

    @JsonProperty("receivablesTurnoverTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<ReceivablesTurnoverTTMQItem> receivablesTurnoverTTMQ;

    @JsonProperty("netMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<NetMarginQItem> netMarginQ;

    @JsonProperty("totalDebtToTotalAsset")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalDebtToTotalAssetQItem> totalDebtToTotalAssetQ;

    @JsonProperty("totalDebtToTotalCapital")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalDebtToTotalCapitalQItem> totalDebtToTotalCapitalQ;

    @JsonProperty("longtermDebtTotalEquity")
    @OneToMany(cascade = CascadeType.ALL)
    private List<LongtermDebtTotalEquityQItem> longtermDebtTotalEquityQ;

    @JsonProperty("roicTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RoicTTMQItem> roicTTMQ;

    @JsonProperty("ev")
    @OneToMany(cascade = CascadeType.ALL)
    private List<EvQItem> evQ;

    @JsonProperty("roeTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RoeTTMQItem> roeTTMQ;

    @JsonProperty("bookValue")
    @OneToMany(cascade = CascadeType.ALL)
    private List<BookValueQItem> bookValueQ;

    @JsonProperty("longtermDebtTotalAsset")
    @OneToMany(cascade = CascadeType.ALL)
    private List<LongtermDebtTotalAssetQItem> longtermDebtTotalAssetQ;

    @JsonProperty("grossMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<GrossMarginQItem> grossMarginQ;

    @JsonProperty("pb")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PbQItem> pbQ;

    @JsonProperty("netDebtToTotalCapital")
    @OneToMany(cascade = CascadeType.ALL)
    private List<NetDebtToTotalCapitalQItem> netDebtToTotalCapitalQ;

    @JsonProperty("ebitPerShare")
    @OneToMany(cascade = CascadeType.ALL)
    private List<EbitPerShareQItem> ebitPerShareQ;

    @JsonProperty("totalDebtToEquity")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalDebtToEquityQItem> totalDebtToEquityQ;

    @JsonProperty("inventoryTurnoverTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<InventoryTurnoverTTMQItem> inventoryTurnoverTTMQ;

    @JsonProperty("currentRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<CurrentRatioQItem> currentRatioQ;

    @JsonProperty("sgaToSale")
    @OneToMany(cascade = CascadeType.ALL)
    private List<SgaToSaleQItem> sgaToSaleQ;

    @JsonProperty("eps")
    @OneToMany(cascade = CascadeType.ALL)
    private List<EpsQItem> epsQ;

    @JsonProperty("cashRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<CashRatioQItem> cashRatioQ;

    @JsonProperty("roaTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RoaTTMQItem> roaTTMQ;

    @JsonProperty("payoutRatioTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PayoutRatioTTMQItem> payoutRatioTTMQ;

    @JsonProperty("psTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PsTTMQItem> psTTMQ;

    @JsonProperty("assetTurnoverTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<AssetTurnoverTTMQItem> assetTurnoverTTMQ;

    @JsonProperty("ptbv")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PtbvQItem> ptbvQ;

    @JsonProperty("tangibleBookValue")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TangibleBookValueQItem> tangibleBookValueQ;

    @JsonProperty("quickRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<QuickRatioQItem> quickRatioQ;

    @JsonProperty("totalRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalRatioQItem> totalRatioQ;

    @JsonProperty("fcfMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<FcfMarginQItem> fcfMarginQ;

    @JsonProperty("longtermDebtTotalCapital")
    @OneToMany(cascade = CascadeType.ALL)
    private List<LongtermDebtTotalCapitalQItem> longtermDebtTotalCapitalQ;

    @JsonProperty("peTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PeTTMQItem> peTTMQ;

    @JsonProperty("operatingMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<OperatingMarginQItem> operatingMarginQ;

    @JsonProperty("pretaxMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PretaxMarginQItem> pretaxMarginQ;

    @JsonProperty("rotcTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RotcTTMQItem> rotcTTMQ;

    @JsonProperty("salesPerShare")
    @OneToMany(cascade = CascadeType.ALL)
    private List<SalesPerShareQItem> salesPerShareQ;

    @JsonProperty("fcfPerShareTTM")
    @OneToMany(cascade = CascadeType.ALL)
    private List<FcfPerShareTTMQItem> fcfPerShareTTMQ;

    @JsonProperty("netDebtToTotalEquity")
    @OneToMany(cascade = CascadeType.ALL)
    private List<NetDebtToTotalEquityQItem> netDebtToTotalEquityQ;

    public List<PfcfTTMQItem> getPfcfTTMQ() {
        return pfcfTTMQ;
    }

    public void setPfcfTTMQ(List<PfcfTTMQItem> pfcfTTMQ) {
        this.pfcfTTMQ = pfcfTTMQ;
    }

    public List<ReceivablesTurnoverTTMQItem> getReceivablesTurnoverTTMQ() {
        return receivablesTurnoverTTMQ;
    }

    public void setReceivablesTurnoverTTMQ(List<ReceivablesTurnoverTTMQItem> receivablesTurnoverTTMQ) {
        this.receivablesTurnoverTTMQ = receivablesTurnoverTTMQ;
    }

    public List<NetMarginQItem> getNetMarginQ() {
        return netMarginQ;
    }

    public void setNetMarginQ(List<NetMarginQItem> netMarginQ) {
        this.netMarginQ = netMarginQ;
    }

    public List<TotalDebtToTotalAssetQItem> getTotalDebtToTotalAssetQ() {
        return totalDebtToTotalAssetQ;
    }

    public void setTotalDebtToTotalAssetQ(List<TotalDebtToTotalAssetQItem> totalDebtToTotalAssetQ) {
        this.totalDebtToTotalAssetQ = totalDebtToTotalAssetQ;
    }

    public List<TotalDebtToTotalCapitalQItem> getTotalDebtToTotalCapitalQ() {
        return totalDebtToTotalCapitalQ;
    }

    public void setTotalDebtToTotalCapitalQ(List<TotalDebtToTotalCapitalQItem> totalDebtToTotalCapitalQ) {
        this.totalDebtToTotalCapitalQ = totalDebtToTotalCapitalQ;
    }

    public List<LongtermDebtTotalEquityQItem> getLongtermDebtTotalEquityQ() {
        return longtermDebtTotalEquityQ;
    }

    public void setLongtermDebtTotalEquityQ(List<LongtermDebtTotalEquityQItem> longtermDebtTotalEquityQ) {
        this.longtermDebtTotalEquityQ = longtermDebtTotalEquityQ;
    }

    public List<RoicTTMQItem> getRoicTTMQ() {
        return roicTTMQ;
    }

    public void setRoicTTMQ(List<RoicTTMQItem> roicTTMQ) {
        this.roicTTMQ = roicTTMQ;
    }

    public List<EvQItem> getEvQ() {
        return evQ;
    }

    public void setEvQ(List<EvQItem> evQ) {
        this.evQ = evQ;
    }

    public List<RoeTTMQItem> getRoeTTMQ() {
        return roeTTMQ;
    }

    public void setRoeTTMQ(List<RoeTTMQItem> roeTTMQ) {
        this.roeTTMQ = roeTTMQ;
    }

    public List<BookValueQItem> getBookValueQ() {
        return bookValueQ;
    }

    public void setBookValueQ(List<BookValueQItem> bookValueQ) {
        this.bookValueQ = bookValueQ;
    }

    public List<LongtermDebtTotalAssetQItem> getLongtermDebtTotalAssetQ() {
        return longtermDebtTotalAssetQ;
    }

    public void setLongtermDebtTotalAssetQ(List<LongtermDebtTotalAssetQItem> longtermDebtTotalAssetQ) {
        this.longtermDebtTotalAssetQ = longtermDebtTotalAssetQ;
    }

    public List<GrossMarginQItem> getGrossMarginQ() {
        return grossMarginQ;
    }

    public void setGrossMarginQ(List<GrossMarginQItem> grossMarginQ) {
        this.grossMarginQ = grossMarginQ;
    }

    public List<PbQItem> getPbQ() {
        return pbQ;
    }

    public void setPbQ(List<PbQItem> pbQ) {
        this.pbQ = pbQ;
    }

    public List<NetDebtToTotalCapitalQItem> getNetDebtToTotalCapitalQ() {
        return netDebtToTotalCapitalQ;
    }

    public void setNetDebtToTotalCapitalQ(List<NetDebtToTotalCapitalQItem> netDebtToTotalCapitalQ) {
        this.netDebtToTotalCapitalQ = netDebtToTotalCapitalQ;
    }

    public List<EbitPerShareQItem> getEbitPerShareQ() {
        return ebitPerShareQ;
    }

    public void setEbitPerShareQ(List<EbitPerShareQItem> ebitPerShareQ) {
        this.ebitPerShareQ = ebitPerShareQ;
    }

    public List<TotalDebtToEquityQItem> getTotalDebtToEquityQ() {
        return totalDebtToEquityQ;
    }

    public void setTotalDebtToEquityQ(List<TotalDebtToEquityQItem> totalDebtToEquityQ) {
        this.totalDebtToEquityQ = totalDebtToEquityQ;
    }

    public List<InventoryTurnoverTTMQItem> getInventoryTurnoverTTMQ() {
        return inventoryTurnoverTTMQ;
    }

    public void setInventoryTurnoverTTMQ(List<InventoryTurnoverTTMQItem> inventoryTurnoverTTMQ) {
        this.inventoryTurnoverTTMQ = inventoryTurnoverTTMQ;
    }

    public List<CurrentRatioQItem> getCurrentRatioQ() {
        return currentRatioQ;
    }

    public void setCurrentRatioQ(List<CurrentRatioQItem> currentRatioQ) {
        this.currentRatioQ = currentRatioQ;
    }

    public List<SgaToSaleQItem> getSgaToSaleQ() {
        return sgaToSaleQ;
    }

    public void setSgaToSaleQ(List<SgaToSaleQItem> sgaToSaleQ) {
        this.sgaToSaleQ = sgaToSaleQ;
    }

    public List<EpsQItem> getEpsQ() {
        return epsQ;
    }

    public void setEpsQ(List<EpsQItem> epsQ) {
        this.epsQ = epsQ;
    }

    public List<CashRatioQItem> getCashRatioQ() {
        return cashRatioQ;
    }

    public void setCashRatioQ(List<CashRatioQItem> cashRatioQ) {
        this.cashRatioQ = cashRatioQ;
    }

    public List<RoaTTMQItem> getRoaTTMQ() {
        return roaTTMQ;
    }

    public void setRoaTTMQ(List<RoaTTMQItem> roaTTMQ) {
        this.roaTTMQ = roaTTMQ;
    }

    public List<PayoutRatioTTMQItem> getPayoutRatioTTMQ() {
        return payoutRatioTTMQ;
    }

    public void setPayoutRatioTTMQ(List<PayoutRatioTTMQItem> payoutRatioTTMQ) {
        this.payoutRatioTTMQ = payoutRatioTTMQ;
    }

    public List<PsTTMQItem> getPsTTMQ() {
        return psTTMQ;
    }

    public void setPsTTMQ(List<PsTTMQItem> psTTMQ) {
        this.psTTMQ = psTTMQ;
    }

    public List<AssetTurnoverTTMQItem> getAssetTurnoverTTMQ() {
        return assetTurnoverTTMQ;
    }

    public void setAssetTurnoverTTMQ(List<AssetTurnoverTTMQItem> assetTurnoverTTMQ) {
        this.assetTurnoverTTMQ = assetTurnoverTTMQ;
    }

    public List<PtbvQItem> getPtbvQ() {
        return ptbvQ;
    }

    public void setPtbvQ(List<PtbvQItem> ptbvQ) {
        this.ptbvQ = ptbvQ;
    }

    public List<TangibleBookValueQItem> getTangibleBookValueQ() {
        return tangibleBookValueQ;
    }

    public void setTangibleBookValueQ(List<TangibleBookValueQItem> tangibleBookValueQ) {
        this.tangibleBookValueQ = tangibleBookValueQ;
    }

    public List<QuickRatioQItem> getQuickRatioQ() {
        return quickRatioQ;
    }

    public void setQuickRatioQ(List<QuickRatioQItem> quickRatioQ) {
        this.quickRatioQ = quickRatioQ;
    }

    public List<TotalRatioQItem> getTotalRatioQ() {
        return totalRatioQ;
    }

    public void setTotalRatioQ(List<TotalRatioQItem> totalRatioQ) {
        this.totalRatioQ = totalRatioQ;
    }

    public List<FcfMarginQItem> getFcfMarginQ() {
        return fcfMarginQ;
    }

    public void setFcfMarginQ(List<FcfMarginQItem> fcfMarginQ) {
        this.fcfMarginQ = fcfMarginQ;
    }

    public List<LongtermDebtTotalCapitalQItem> getLongtermDebtTotalCapitalQ() {
        return longtermDebtTotalCapitalQ;
    }

    public void setLongtermDebtTotalCapitalQ(List<LongtermDebtTotalCapitalQItem> longtermDebtTotalCapitalQ) {
        this.longtermDebtTotalCapitalQ = longtermDebtTotalCapitalQ;
    }

    public List<PeTTMQItem> getPeTTMQ() {
        return peTTMQ;
    }

    public void setPeTTMQ(List<PeTTMQItem> peTTMQ) {
        this.peTTMQ = peTTMQ;
    }

    public List<OperatingMarginQItem> getOperatingMarginQ() {
        return operatingMarginQ;
    }

    public void setOperatingMarginQ(List<OperatingMarginQItem> operatingMarginQ) {
        this.operatingMarginQ = operatingMarginQ;
    }

    public List<PretaxMarginQItem> getPretaxMarginQ() {
        return pretaxMarginQ;
    }

    public void setPretaxMarginQ(List<PretaxMarginQItem> pretaxMarginQ) {
        this.pretaxMarginQ = pretaxMarginQ;
    }

    public List<RotcTTMQItem> getRotcTTMQ() {
        return rotcTTMQ;
    }

    public void setRotcTTMQ(List<RotcTTMQItem> rotcTTMQ) {
        this.rotcTTMQ = rotcTTMQ;
    }

    public List<SalesPerShareQItem> getSalesPerShareQ() {
        return salesPerShareQ;
    }

    public void setSalesPerShareQ(List<SalesPerShareQItem> salesPerShareQ) {
        this.salesPerShareQ = salesPerShareQ;
    }

    public List<FcfPerShareTTMQItem> getFcfPerShareTTMQ() {
        return fcfPerShareTTMQ;
    }

    public void setFcfPerShareTTMQ(List<FcfPerShareTTMQItem> fcfPerShareTTMQ) {
        this.fcfPerShareTTMQ = fcfPerShareTTMQ;
    }

    public List<NetDebtToTotalEquityQItem> getNetDebtToTotalEquityQ() {
        return netDebtToTotalEquityQ;
    }

    public void setNetDebtToTotalEquityQ(List<NetDebtToTotalEquityQItem> netDebtToTotalEquityQ) {
        this.netDebtToTotalEquityQ = netDebtToTotalEquityQ;
    }

    @Override
    public String toString() {
        return
                "Quarterly{" +
                        ",pfcfTTM_q = '" + pfcfTTMQ + '\'' +
                        ",receivablesTurnoverTTM_q = '" + receivablesTurnoverTTMQ + '\'' +
                        ",netMargin_q = '" + netMarginQ + '\'' +
                        ",totalDebtToTotalAsset_q = '" + totalDebtToTotalAssetQ + '\'' +
                        ",totalDebtToTotalCapital_q = '" + totalDebtToTotalCapitalQ + '\'' +
                        ",longtermDebtTotalEquity_q = '" + longtermDebtTotalEquityQ + '\'' +
                        ",roicTTM_q = '" + roicTTMQ + '\'' +
                        ",ev_q = '" + evQ + '\'' +
                        ",roeTTM_q = '" + roeTTMQ + '\'' +
                        ",bookValue_q = '" + bookValueQ + '\'' +
                        ",longtermDebtTotalAsset_q = '" + longtermDebtTotalAssetQ + '\'' +
                        ",grossMargin_q = '" + grossMarginQ + '\'' +
                        ",pb_q = '" + pbQ + '\'' +
                        ",netDebtToTotalCapital_q = '" + netDebtToTotalCapitalQ + '\'' +
                        ",ebitPerShare_q = '" + ebitPerShareQ + '\'' +
                        ",totalDebtToEquity_q = '" + totalDebtToEquityQ + '\'' +
                        ",inventoryTurnoverTTM_q = '" + inventoryTurnoverTTMQ + '\'' +
                        ",currentRatio_q = '" + currentRatioQ + '\'' +
                        ",sgaToSale_q = '" + sgaToSaleQ + '\'' +
                        ",eps_q = '" + epsQ + '\'' +
                        ",cashRatio_q = '" + cashRatioQ + '\'' +
                        ",roaTTM_q = '" + roaTTMQ + '\'' +
                        ",payoutRatioTTM_q = '" + payoutRatioTTMQ + '\'' +
                        ",psTTM_q = '" + psTTMQ + '\'' +
                        ",assetTurnoverTTM_q = '" + assetTurnoverTTMQ + '\'' +
                        ",ptbv_q = '" + ptbvQ + '\'' +
                        ",tangibleBookValue_q = '" + tangibleBookValueQ + '\'' +
                        ",quickRatio_q = '" + quickRatioQ + '\'' +
                        ",totalRatio_q = '" + totalRatioQ + '\'' +
                        ",fcfMargin_q = '" + fcfMarginQ + '\'' +
                        ",longtermDebtTotalCapital_q = '" + longtermDebtTotalCapitalQ + '\'' +
                        ",peTTM_q = '" + peTTMQ + '\'' +
                        ",operatingMargin_q = '" + operatingMarginQ + '\'' +
                        ",pretaxMargin_q = '" + pretaxMarginQ + '\'' +
                        ",rotcTTM_q = '" + rotcTTMQ + '\'' +
                        ",salesPerShare_q = '" + salesPerShareQ + '\'' +
                        ",fcfPerShareTTM_q = '" + fcfPerShareTTMQ + '\'' +
                        ",netDebtToTotalEquity_q = '" + netDebtToTotalEquityQ + '\'' +
                        "}";
    }
}