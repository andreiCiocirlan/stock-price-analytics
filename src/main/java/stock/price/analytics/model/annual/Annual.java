package stock.price.analytics.model.annual;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.util.List;

//@Embeddable
public class Annual {

    @JsonProperty("operatingMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<OperatingMarginItem> operatingMargin;

    @JsonProperty("ps")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PsItem> ps;

    @JsonProperty("quickRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<QuickRatioItem> quickRatio;

    @JsonProperty("roa")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RoaItem> roa;

    @JsonProperty("cashRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<CashRatioItem> cashRatio;

    @JsonProperty("roe")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RoeItem> roe;

    @JsonProperty("pretaxMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PretaxMarginItem> pretaxMargin;

    @JsonProperty("fcfMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<FcfMarginItem> fcfMargin;

    @JsonProperty("totalRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalRatioItem> totalRatio;

    @JsonProperty("totalDebtToEquity")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalDebtToEquityItem> totalDebtToEquity;

    @JsonProperty("totalDebtToTotalCapital")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalDebtToTotalCapitalItem> totalDebtToTotalCapital;

    @JsonProperty("rotc")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RotcItem> rotc;

    @JsonProperty("currentRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<CurrentRatioItem> currentRatio;

    @JsonProperty("longtermDebtTotalCapital")
    @OneToMany(cascade = CascadeType.ALL)
    private List<LongtermDebtTotalCapitalItem> longtermDebtTotalCapital;

    @JsonProperty("bookValue")
    @OneToMany(cascade = CascadeType.ALL)
    private List<BookValueItem> bookValue;

    @JsonProperty("tangibleBookValue")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TangibleBookValueItem> tangibleBookValue;

    @JsonProperty("roic")
    @OneToMany(cascade = CascadeType.ALL)
    private List<RoicItem> roic;

    @JsonProperty("sgaToSale")
    @OneToMany(cascade = CascadeType.ALL)
    private List<SgaToSaleItem> sgaToSale;

    @JsonProperty("ebitPerShare")
    @OneToMany(cascade = CascadeType.ALL)
    private List<EbitPerShareItem> ebitPerShare;

    @JsonProperty("inventoryTurnover")
    @OneToMany(cascade = CascadeType.ALL)
    private List<InventoryTurnoverItem> inventoryTurnover;

    @JsonProperty("eps")
    @OneToMany(cascade = CascadeType.ALL)
    private List<EpsItem> eps;

    @JsonProperty("netMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<NetMarginItem> netMargin;

    @JsonProperty("ptbv")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PtbvItem> ptbv;

    @JsonProperty("receivablesTurnover")
    @OneToMany(cascade = CascadeType.ALL)
    private List<ReceivablesTurnoverItem> receivablesTurnover;

    @JsonProperty("pfcf")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PfcfItem> pfcf;

    @JsonProperty("netDebtToTotalEquity")
    @OneToMany(cascade = CascadeType.ALL)
    private List<NetDebtToTotalEquityItem> netDebtToTotalEquity;

    @JsonProperty("ev")
    @OneToMany(cascade = CascadeType.ALL)
    private List<EvItem> ev;

    @JsonProperty("pb")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PbItem> pb;

    @JsonProperty("salesPerShare")
    @OneToMany(cascade = CascadeType.ALL)
    private List<SalesPerShareItem> salesPerShare;

    @JsonProperty("pe")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PeItem> pe;

    @JsonProperty("longtermDebtTotalAsset")
    @OneToMany(cascade = CascadeType.ALL)
    private List<LongtermDebtTotalAssetItem> longtermDebtTotalAsset;

    @JsonProperty("totalDebtToTotalAsset")
    @OneToMany(cascade = CascadeType.ALL)
    private List<TotalDebtToTotalAssetItem> totalDebtToTotalAsset;

    @JsonProperty("longtermDebtTotalEquity")
    @OneToMany(cascade = CascadeType.ALL)
    private List<LongtermDebtTotalEquityItem> longtermDebtTotalEquity;

    @JsonProperty("grossMargin")
    @OneToMany(cascade = CascadeType.ALL)
    private List<GrossMarginItem> grossMargin;

    @JsonProperty("payoutRatio")
    @OneToMany(cascade = CascadeType.ALL)
    private List<PayoutRatioItem> payoutRatio;

    @JsonProperty("netDebtToTotalCapital")
    @OneToMany(cascade = CascadeType.ALL)
    private List<NetDebtToTotalCapitalItem> netDebtToTotalCapital;

    public void setOperatingMargin(List<OperatingMarginItem> operatingMargin) {
        this.operatingMargin = operatingMargin;
    }

    public List<OperatingMarginItem> getOperatingMargin() {
        return operatingMargin;
    }

    public void setPs(List<PsItem> ps) {
        this.ps = ps;
    }

    public List<PsItem> getPs() {
        return ps;
    }

    public void setQuickRatio(List<QuickRatioItem> quickRatio) {
        this.quickRatio = quickRatio;
    }

    public List<QuickRatioItem> getQuickRatio() {
        return quickRatio;
    }

    public void setRoa(List<RoaItem> roa) {
        this.roa = roa;
    }

    public List<RoaItem> getRoa() {
        return roa;
    }

    public void setCashRatio(List<CashRatioItem> cashRatio) {
        this.cashRatio = cashRatio;
    }

    public List<CashRatioItem> getCashRatio() {
        return cashRatio;
    }

    public void setRoe(List<RoeItem> roe) {
        this.roe = roe;
    }

    public List<RoeItem> getRoe() {
        return roe;
    }

    public void setPretaxMargin(List<PretaxMarginItem> pretaxMargin) {
        this.pretaxMargin = pretaxMargin;
    }

    public List<PretaxMarginItem> getPretaxMargin() {
        return pretaxMargin;
    }

    public void setFcfMargin(List<FcfMarginItem> fcfMargin) {
        this.fcfMargin = fcfMargin;
    }

    public List<FcfMarginItem> getFcfMargin() {
        return fcfMargin;
    }

    public void setTotalRatio(List<TotalRatioItem> totalRatio) {
        this.totalRatio = totalRatio;
    }

    public List<TotalRatioItem> getTotalRatio() {
        return totalRatio;
    }

    public void setTotalDebtToEquity(List<TotalDebtToEquityItem> totalDebtToEquity) {
        this.totalDebtToEquity = totalDebtToEquity;
    }

    public List<TotalDebtToEquityItem> getTotalDebtToEquity() {
        return totalDebtToEquity;
    }

    public void setTotalDebtToTotalCapital(List<TotalDebtToTotalCapitalItem> totalDebtToTotalCapital) {
        this.totalDebtToTotalCapital = totalDebtToTotalCapital;
    }

    public List<TotalDebtToTotalCapitalItem> getTotalDebtToTotalCapital() {
        return totalDebtToTotalCapital;
    }

    public void setRotc(List<RotcItem> rotc) {
        this.rotc = rotc;
    }

    public List<RotcItem> getRotc() {
        return rotc;
    }

    public void setCurrentRatio(List<CurrentRatioItem> currentRatio) {
        this.currentRatio = currentRatio;
    }

    public List<CurrentRatioItem> getCurrentRatio() {
        return currentRatio;
    }

    public void setLongtermDebtTotalCapital(List<LongtermDebtTotalCapitalItem> longtermDebtTotalCapital) {
        this.longtermDebtTotalCapital = longtermDebtTotalCapital;
    }

    public List<LongtermDebtTotalCapitalItem> getLongtermDebtTotalCapital() {
        return longtermDebtTotalCapital;
    }

    public void setBookValue(List<BookValueItem> bookValue) {
        this.bookValue = bookValue;
    }

    public List<BookValueItem> getBookValue() {
        return bookValue;
    }

    public void setTangibleBookValue(List<TangibleBookValueItem> tangibleBookValue) {
        this.tangibleBookValue = tangibleBookValue;
    }

    public List<TangibleBookValueItem> getTangibleBookValue() {
        return tangibleBookValue;
    }

    public void setRoic(List<RoicItem> roic) {
        this.roic = roic;
    }

    public List<RoicItem> getRoic() {
        return roic;
    }

    public void setSgaToSale(List<SgaToSaleItem> sgaToSale) {
        this.sgaToSale = sgaToSale;
    }

    public List<SgaToSaleItem> getSgaToSale() {
        return sgaToSale;
    }

    public void setEbitPerShare(List<EbitPerShareItem> ebitPerShare) {
        this.ebitPerShare = ebitPerShare;
    }

    public List<EbitPerShareItem> getEbitPerShare() {
        return ebitPerShare;
    }

    public void setInventoryTurnover(List<InventoryTurnoverItem> inventoryTurnover) {
        this.inventoryTurnover = inventoryTurnover;
    }

    public List<InventoryTurnoverItem> getInventoryTurnover() {
        return inventoryTurnover;
    }

    public void setEps(List<EpsItem> eps) {
        this.eps = eps;
    }

    public List<EpsItem> getEps() {
        return eps;
    }

    public void setNetMargin(List<NetMarginItem> netMargin) {
        this.netMargin = netMargin;
    }

    public List<NetMarginItem> getNetMargin() {
        return netMargin;
    }

    public void setPtbv(List<PtbvItem> ptbv) {
        this.ptbv = ptbv;
    }

    public List<PtbvItem> getPtbv() {
        return ptbv;
    }

    public void setReceivablesTurnover(List<ReceivablesTurnoverItem> receivablesTurnover) {
        this.receivablesTurnover = receivablesTurnover;
    }

    public List<ReceivablesTurnoverItem> getReceivablesTurnover() {
        return receivablesTurnover;
    }

    public void setPfcf(List<PfcfItem> pfcf) {
        this.pfcf = pfcf;
    }

    public List<PfcfItem> getPfcf() {
        return pfcf;
    }

    public void setNetDebtToTotalEquity(List<NetDebtToTotalEquityItem> netDebtToTotalEquity) {
        this.netDebtToTotalEquity = netDebtToTotalEquity;
    }

    public List<NetDebtToTotalEquityItem> getNetDebtToTotalEquity() {
        return netDebtToTotalEquity;
    }

    public void setEv(List<EvItem> ev) {
        this.ev = ev;
    }

    public List<EvItem> getEv() {
        return ev;
    }

    public void setPb(List<PbItem> pb) {
        this.pb = pb;
    }

    public List<PbItem> getPb() {
        return pb;
    }

    public void setSalesPerShare(List<SalesPerShareItem> salesPerShare) {
        this.salesPerShare = salesPerShare;
    }

    public List<SalesPerShareItem> getSalesPerShare() {
        return salesPerShare;
    }

    public void setPe(List<PeItem> pe) {
        this.pe = pe;
    }

    public List<PeItem> getPe() {
        return pe;
    }

    public void setLongtermDebtTotalAsset(List<LongtermDebtTotalAssetItem> longtermDebtTotalAsset) {
        this.longtermDebtTotalAsset = longtermDebtTotalAsset;
    }

    public List<LongtermDebtTotalAssetItem> getLongtermDebtTotalAsset() {
        return longtermDebtTotalAsset;
    }

    public void setTotalDebtToTotalAsset(List<TotalDebtToTotalAssetItem> totalDebtToTotalAsset) {
        this.totalDebtToTotalAsset = totalDebtToTotalAsset;
    }

    public List<TotalDebtToTotalAssetItem> getTotalDebtToTotalAsset() {
        return totalDebtToTotalAsset;
    }

    public void setLongtermDebtTotalEquity(List<LongtermDebtTotalEquityItem> longtermDebtTotalEquity) {
        this.longtermDebtTotalEquity = longtermDebtTotalEquity;
    }

    public List<LongtermDebtTotalEquityItem> getLongtermDebtTotalEquity() {
        return longtermDebtTotalEquity;
    }

    public void setGrossMargin(List<GrossMarginItem> grossMargin) {
        this.grossMargin = grossMargin;
    }

    public List<GrossMarginItem> getGrossMargin() {
        return grossMargin;
    }

    public void setPayoutRatio(List<PayoutRatioItem> payoutRatio) {
        this.payoutRatio = payoutRatio;
    }

    public List<PayoutRatioItem> getPayoutRatio() {
        return payoutRatio;
    }

    public void setNetDebtToTotalCapital(List<NetDebtToTotalCapitalItem> netDebtToTotalCapital) {
        this.netDebtToTotalCapital = netDebtToTotalCapital;
    }

    public List<NetDebtToTotalCapitalItem> getNetDebtToTotalCapital() {
        return netDebtToTotalCapital;
    }

    @Override
    public String toString() {
        return
                "Annual{" +
                        "operatingMargin = '" + operatingMargin + '\'' +
                        ",ps = '" + ps + '\'' +
                        ",quickRatio = '" + quickRatio + '\'' +
                        ",roa = '" + roa + '\'' +
                        ",cashRatio = '" + cashRatio + '\'' +
                        ",roe = '" + roe + '\'' +
                        ",pretaxMargin = '" + pretaxMargin + '\'' +
                        ",fcfMargin = '" + fcfMargin + '\'' +
                        ",totalRatio = '" + totalRatio + '\'' +
                        ",totalDebtToEquity = '" + totalDebtToEquity + '\'' +
                        ",totalDebtToTotalCapital = '" + totalDebtToTotalCapital + '\'' +
                        ",rotc = '" + rotc + '\'' +
                        ",currentRatio = '" + currentRatio + '\'' +
                        ",longtermDebtTotalCapital = '" + longtermDebtTotalCapital + '\'' +
                        ",bookValue = '" + bookValue + '\'' +
                        ",tangibleBookValue = '" + tangibleBookValue + '\'' +
                        ",roic = '" + roic + '\'' +
                        ",sgaToSale = '" + sgaToSale + '\'' +
                        ",ebitPerShare = '" + ebitPerShare + '\'' +
                        ",inventoryTurnover = '" + inventoryTurnover + '\'' +
                        ",eps = '" + eps + '\'' +
                        ",netMargin = '" + netMargin + '\'' +
                        ",ptbv = '" + ptbv + '\'' +
                        ",receivablesTurnover = '" + receivablesTurnover + '\'' +
                        ",pfcf = '" + pfcf + '\'' +
                        ",netDebtToTotalEquity = '" + netDebtToTotalEquity + '\'' +
                        ",ev = '" + ev + '\'' +
                        ",pb = '" + pb + '\'' +
                        ",salesPerShare = '" + salesPerShare + '\'' +
                        ",pe = '" + pe + '\'' +
                        ",longtermDebtTotalAsset = '" + longtermDebtTotalAsset + '\'' +
                        ",totalDebtToTotalAsset = '" + totalDebtToTotalAsset + '\'' +
                        ",longtermDebtTotalEquity = '" + longtermDebtTotalEquity + '\'' +
                        ",grossMargin = '" + grossMargin + '\'' +
                        ",payoutRatio = '" + payoutRatio + '\'' +
                        ",netDebtToTotalCapital = '" + netDebtToTotalCapital + '\'' +
                        "}";
    }
}