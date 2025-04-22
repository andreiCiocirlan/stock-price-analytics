package stock.price.analytics.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import stock.price.analytics.model.BusinessEntity;
import stock.price.analytics.model.prices.ohlc.CandleOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Getter
@Setter
@Entity
@DynamicUpdate
@NoArgsConstructor
@Table(name = "daily_prices_json")
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyPriceJSON implements BusinessEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_prices_json_gen")
	@SequenceGenerator(name = "daily_prices_json_gen", sequenceName = "daily_prices_json_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("twoHundredDayAverageChangePercent")
	private Double twoHundredDayAverageChangePercent;

	@JsonProperty("averageAnalystRating")
	private String averageAnalystRating;

	@JsonProperty("fiftyTwoWeekLowChangePercent")
	private Double fiftyTwoWeekLowChangePercent;

	@JsonProperty("isEarningsDateEstimate")
	private boolean isEarningsDateEstimate;

	@JsonProperty("language")
	private String language;

	@JsonProperty("regularMarketDayRange")
	private String regularMarketDayRange;

	@JsonProperty("earningsTimestampEnd")
	private Long earningsTimestampEnd;

	@JsonProperty("epsForward")
	private Double epsForward;

	@JsonProperty("regularMarketDayHigh")
	private Double regularMarketDayHigh;

	@JsonProperty("twoHundredDayAverageChange")
	private Double twoHundredDayAverageChange;

	@JsonProperty("askSize")
	private Long askSize;

	@JsonProperty("twoHundredDayAverage")
	private Double twoHundredDayAverage;

	@JsonProperty("bookValue")
	private Double bookValue;

	@JsonProperty("fiftyTwoWeekHighChange")
	private Double fiftyTwoWeekHighChange;

	@JsonProperty("marketCap")
	private Long marketCap;

	@JsonProperty("esgPopulated")
	private boolean esgPopulated;

	@JsonProperty("fiftyTwoWeekRange")
	private String fiftyTwoWeekRange;

	@JsonProperty("fiftyDayAverageChange")
	private Double fiftyDayAverageChange;

	@JsonProperty("exchangeDataDelayedBy")
	private Long exchangeDataDelayedBy;

	@JsonProperty("firstTradeDateMilliseconds")
	private Long firstTradeDateMilliseconds;

	@JsonProperty("averageDailyVolume3Month")
	private Long averageDailyVolume3Month;

	@JsonProperty("fiftyTwoWeekChangePercent")
	private Double fiftyTwoWeekChangePercent;

	@JsonProperty("trailingAnnualDividendRate")
	private Double trailingAnnualDividendRate;

	@JsonProperty("hasPrePostMarketData")
	private boolean hasPrePostMarketData;

	@JsonProperty("fiftyTwoWeekLow")
	private Double fiftyTwoWeekLow;

	@JsonProperty("market")
	private String market;

	@JsonProperty("regularMarketVolume")
	private Long regularMarketVolume;

	@JsonProperty("postMarketPrice")
	private Double postMarketPrice;

	@JsonProperty("quoteSourceName")
	private String quoteSourceName;

	@JsonProperty("messageBoardId")
	private String messageBoardId;

	@JsonProperty("priceHint")
	private Long priceHint;

	@JsonProperty("exchange")
	private String exchange;

	@JsonProperty("sourceInterval")
	private Long sourceInterval;

	@JsonProperty("regularMarketDayLow")
	private Double regularMarketDayLow;

	@JsonProperty("region")
	private String region;

	@JsonProperty("shortName")
	private String shortName;

	@JsonProperty("fiftyDayAverageChangePercent")
	private Double fiftyDayAverageChangePercent;

	@JsonProperty("fullExchangeName")
	private String fullExchangeName;

	@JsonProperty("earningsTimestampStart")
	private Long earningsTimestampStart;

	@JsonProperty("financialCurrency")
	private String financialCurrency;

	@JsonProperty("displayName")
	private String displayName;

	@JsonProperty("gmtOffSetMilliseconds")
	private Long gmtOffSetMilliseconds;

	@JsonProperty("regularMarketOpen")
	private Double regularMarketOpen;

	@Column(name = "date")
	@JsonProperty("regularMarketTime")
	private LocalDate date;

	@JsonProperty("regularMarketChangePercent")
	private Double regularMarketChangePercent;

	@JsonProperty("quoteType")
	private String quoteType;

	@JsonProperty("trailingAnnualDividendYield")
	private Double trailingAnnualDividendYield;

	@JsonProperty("averageDailyVolume10Day")
	private Long averageDailyVolume10Day;

	@JsonProperty("fiftyTwoWeekLowChange")
	private Double fiftyTwoWeekLowChange;

	@JsonProperty("fiftyTwoWeekHighChangePercent")
	private Double fiftyTwoWeekHighChangePercent;

	@JsonProperty("earningsCallTimestampEnd")
	private Long earningsCallTimestampEnd;

	@JsonProperty("typeDisp")
	private String typeDisp;

	@JsonProperty("trailingPE")
	private Double trailingPE;

	@JsonProperty("tradeable")
	private boolean tradeable;

	@JsonProperty("postMarketTime")
	private Long postMarketTime;

	@JsonProperty("currency")
	private String currency;

	@JsonProperty("sharesOutstanding")
	private Long sharesOutstanding;

	@JsonProperty("regularMarketPreviousClose")
	private Double regularMarketPreviousClose;

	@JsonProperty("fiftyTwoWeekHigh")
	private Double fiftyTwoWeekHigh;

	@JsonProperty("exchangeTimezoneName")
	private String exchangeTimezoneName;

	@JsonProperty("postMarketChangePercent")
	private Double postMarketChangePercent;

	@JsonProperty("regularMarketChange")
	private Double regularMarketChange;

	@JsonProperty("bidSize")
	private Long bidSize;

	@JsonProperty("priceEpsCurrentYear")
	private Double priceEpsCurrentYear;

	@JsonProperty("cryptoTradeable")
	private boolean cryptoTradeable;

	@JsonProperty("fiftyDayAverage")
	private Double fiftyDayAverage;

	@JsonProperty("exchangeTimezoneShortName")
	private String exchangeTimezoneShortName;

	@JsonProperty("epsCurrentYear")
	private Double epsCurrentYear;

	@JsonProperty("customPriceAlertConfidence")
	private String customPriceAlertConfidence;

	@JsonProperty("regularMarketPrice")
	private Double regularMarketPrice;

	@JsonProperty("marketState")
	@Enumerated(EnumType.STRING)
	private MarketState marketState;

	@JsonProperty("postMarketChange")
	private Double postMarketChange;

	@JsonProperty("forwardPE")
	private Double forwardPE;

	@JsonProperty("earningsTimestamp")
	private Long earningsTimestamp;

	@JsonProperty("ask")
	private Double ask;

	@JsonProperty("epsTrailingTwelveMonths")
	private Double epsTrailingTwelveMonths;

	@JsonProperty("bid")
	private Double bid;

	@JsonProperty("triggerable")
	private boolean triggerable;

	@JsonProperty("earningsCallTimestampStart")
	private Long earningsCallTimestampStart;

	@JsonProperty("priceToBook")
	private Double priceToBook;

	@JsonProperty("longName")
	private String longName;

	@JsonProperty("dividendDate")
	private Long dividendDate;

	@JsonProperty("dividendYield")
	private Double dividendYield;

	@JsonProperty("dividendRate")
	private Double dividendRate;

	@JsonProperty("prevName")
	private String prevName;

	@JsonProperty("prevExchange")
	private String prevExchange;

	@JsonProperty("exchangeTransferDate")
	private String exchangeTransferDate;

	@JsonProperty("preMarketTime")
	private LocalDate preMarketTime;

	@JsonProperty("preMarketPrice")
	private Double preMarketPrice;

	@JsonProperty("preMarketChange")
	private Double preMarketChange;

	@JsonProperty("preMarketChangePercent")
	private Double preMarketChangePercent;

	@JsonProperty("nameChangeDate")
	private String nameChangeDate;

	@JsonProperty("ipoExpectedDate")
	private String ipoExpectedDate;

	@JsonProperty("openInterest")
	private Long openInterest;

	@Column(name = "prevTicker")
	@JsonProperty("prevTicker")
	private String prevTicker;

	@Column(name = "newListingDate")
	@JsonProperty("newListingDate")
	private String newListingDate;

	@Column(name = "tickerChangeDate")
	@JsonProperty("tickerChangeDate")
	private String tickerChangeDate;

	@Column(name = "delistingDate")
	@JsonProperty("delistingDate")
	private String delistingDate;

	public String getCompositeId() {
		return getSymbol() + "_" + getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	public DailyPrice convertToDailyPrice(boolean preMarket) {
        double open = this.getRegularMarketOpen();
		double high = this.getRegularMarketDayHigh();
		double low = this.getRegularMarketDayLow();
		double close = preMarket ? this.getPreMarketPrice() : this.getRegularMarketPrice();
		double percentChange = preMarket ? this.getPreMarketChangePercent() : this.getRegularMarketChangePercent();

		return new DailyPrice(this.getSymbol(), this.getDate(), percentChange, new CandleOHLC(open, high, low, close));
	}

	public DailyPriceJSON toLighterVersion() {
		DailyPriceJSON lighter = new DailyPriceJSON();
		lighter.setRegularMarketOpen(this.getRegularMarketOpen());
		lighter.setRegularMarketDayHigh(this.getRegularMarketDayHigh());
		lighter.setRegularMarketDayLow(this.getRegularMarketDayLow());
		lighter.setRegularMarketPrice(this.getRegularMarketPrice());
		lighter.setRegularMarketChangePercent(this.getRegularMarketChangePercent());
		lighter.setSymbol(this.getSymbol());
		lighter.setDate(this.getDate());
		lighter.setPreMarketPrice(this.getPreMarketPrice());
		lighter.setFiftyDayAverage(this.getFiftyDayAverage());
		lighter.setTwoHundredDayAverage(this.getTwoHundredDayAverage());

		return lighter;
	}

	public DailyPriceJSON updateFrom(DailyPriceJSON dailyPriceJSON) {
		this.symbol = dailyPriceJSON.getSymbol();
		this.twoHundredDayAverageChangePercent = dailyPriceJSON.getTwoHundredDayAverageChangePercent();
		this.averageAnalystRating = dailyPriceJSON.getAverageAnalystRating();
		this.fiftyTwoWeekLowChangePercent = dailyPriceJSON.getFiftyTwoWeekLowChangePercent();
		this.isEarningsDateEstimate = dailyPriceJSON.isEarningsDateEstimate();
		this.language = dailyPriceJSON.getLanguage();
		this.regularMarketDayRange = dailyPriceJSON.getRegularMarketDayRange();
		this.earningsTimestampEnd = dailyPriceJSON.getEarningsTimestampEnd();
		this.epsForward = dailyPriceJSON.getEpsForward();
		this.twoHundredDayAverageChange = dailyPriceJSON.getTwoHundredDayAverageChange();
		this.askSize = dailyPriceJSON.getAskSize();
		this.twoHundredDayAverage = dailyPriceJSON.getTwoHundredDayAverage();
		this.bookValue = dailyPriceJSON.getBookValue();
		this.fiftyTwoWeekHighChange = dailyPriceJSON.getFiftyTwoWeekHighChange();
		this.marketCap = dailyPriceJSON.getMarketCap();
		this.esgPopulated = dailyPriceJSON.isEsgPopulated();
		this.fiftyTwoWeekRange = dailyPriceJSON.getFiftyTwoWeekRange();
		this.fiftyDayAverageChange = dailyPriceJSON.getFiftyDayAverageChange();
		this.exchangeDataDelayedBy = dailyPriceJSON.getExchangeDataDelayedBy();
		this.firstTradeDateMilliseconds = dailyPriceJSON.getFirstTradeDateMilliseconds();
		this.averageDailyVolume3Month = dailyPriceJSON.getAverageDailyVolume3Month();
		this.fiftyTwoWeekChangePercent = dailyPriceJSON.getFiftyTwoWeekChangePercent();
		this.trailingAnnualDividendRate = dailyPriceJSON.getTrailingAnnualDividendRate();
		this.hasPrePostMarketData = dailyPriceJSON.isHasPrePostMarketData();
		this.fiftyTwoWeekLow = dailyPriceJSON.getFiftyTwoWeekLow();
		this.market = dailyPriceJSON.getMarket();
		this.regularMarketVolume = dailyPriceJSON.getRegularMarketVolume();
		this.postMarketPrice = dailyPriceJSON.getPostMarketPrice();
		this.quoteSourceName = dailyPriceJSON.getQuoteSourceName();
		this.messageBoardId = dailyPriceJSON.getMessageBoardId();
		this.priceHint = dailyPriceJSON.getPriceHint();
		this.exchange = dailyPriceJSON.getExchange();
		this.sourceInterval = dailyPriceJSON.getSourceInterval();
		this.region = dailyPriceJSON.getRegion();
		this.shortName = dailyPriceJSON.getShortName();
		this.fiftyDayAverageChangePercent = dailyPriceJSON.getFiftyDayAverageChangePercent();
		this.fullExchangeName = dailyPriceJSON.getFullExchangeName();
		this.earningsTimestampStart = dailyPriceJSON.getEarningsTimestampStart();
		this.financialCurrency = dailyPriceJSON.getFinancialCurrency();
		this.displayName = dailyPriceJSON.getDisplayName();
		this.gmtOffSetMilliseconds = dailyPriceJSON.getGmtOffSetMilliseconds();
		this.regularMarketOpen = dailyPriceJSON.getRegularMarketOpen();
		this.date = dailyPriceJSON.getDate();
		this.regularMarketChangePercent = dailyPriceJSON.getRegularMarketChangePercent();
		this.quoteType = dailyPriceJSON.getQuoteType();
		this.trailingAnnualDividendYield = dailyPriceJSON.getTrailingAnnualDividendYield();
		this.averageDailyVolume10Day = dailyPriceJSON.getAverageDailyVolume10Day();
		this.fiftyTwoWeekLowChange = dailyPriceJSON.getFiftyTwoWeekLowChange();
		this.fiftyTwoWeekHighChangePercent = dailyPriceJSON.getFiftyTwoWeekHighChangePercent();
		this.earningsCallTimestampEnd = dailyPriceJSON.getEarningsCallTimestampEnd();
		this.typeDisp = dailyPriceJSON.getTypeDisp();
		this.trailingPE = dailyPriceJSON.getTrailingPE();
		this.tradeable = dailyPriceJSON.isTradeable();
		this.postMarketTime = dailyPriceJSON.getPostMarketTime();
		this.currency = dailyPriceJSON.getCurrency();
		this.sharesOutstanding = dailyPriceJSON.getSharesOutstanding();
		this.regularMarketPreviousClose = dailyPriceJSON.getRegularMarketPreviousClose();
		this.fiftyTwoWeekHigh = dailyPriceJSON.getFiftyTwoWeekHigh();
		this.exchangeTimezoneName = dailyPriceJSON.getExchangeTimezoneName();
		this.postMarketChangePercent = dailyPriceJSON.getPostMarketChangePercent();
		this.regularMarketChange = dailyPriceJSON.getRegularMarketChange();
		this.bidSize = dailyPriceJSON.getBidSize();
		this.priceEpsCurrentYear = dailyPriceJSON.getPriceEpsCurrentYear();
		this.cryptoTradeable = dailyPriceJSON.isCryptoTradeable();
		this.fiftyDayAverage = dailyPriceJSON.getFiftyDayAverage();
		this.exchangeTimezoneShortName = dailyPriceJSON.getExchangeTimezoneShortName();
		this.epsCurrentYear = dailyPriceJSON.getEpsCurrentYear();
		this.customPriceAlertConfidence = dailyPriceJSON.getCustomPriceAlertConfidence();
		this.regularMarketPrice = dailyPriceJSON.getRegularMarketPrice();
		this.marketState = dailyPriceJSON.getMarketState();
		this.postMarketChange = dailyPriceJSON.getPostMarketChange();
		this.forwardPE = dailyPriceJSON.getForwardPE();
		this.earningsTimestamp = dailyPriceJSON.getEarningsTimestamp();
		this.ask = dailyPriceJSON.getAsk();
		this.epsTrailingTwelveMonths = dailyPriceJSON.getEpsTrailingTwelveMonths();
		this.bid = dailyPriceJSON.getBid();
		this.triggerable = dailyPriceJSON.isTriggerable();
		this.earningsCallTimestampStart = dailyPriceJSON.getEarningsCallTimestampStart();
		this.priceToBook = dailyPriceJSON.getPriceToBook();
		this.longName = dailyPriceJSON.getLongName();
		this.dividendDate = dailyPriceJSON.getDividendDate();
		this.dividendYield = dailyPriceJSON.getDividendYield();
		this.dividendRate = dailyPriceJSON.getDividendRate();
		this.prevName = dailyPriceJSON.getPrevName();
		this.prevExchange = dailyPriceJSON.getPrevExchange();
		this.exchangeTransferDate = dailyPriceJSON.getExchangeTransferDate();
		this.preMarketTime = dailyPriceJSON.getPreMarketTime();
		this.preMarketPrice = dailyPriceJSON.getPreMarketPrice();
		this.preMarketChange = dailyPriceJSON.getPreMarketChange();
		this.preMarketChangePercent = dailyPriceJSON.getPreMarketChangePercent();
		this.nameChangeDate = dailyPriceJSON.getNameChangeDate();
		this.ipoExpectedDate = dailyPriceJSON.getIpoExpectedDate();
		this.openInterest = dailyPriceJSON.getOpenInterest();
		this.prevTicker = dailyPriceJSON.getPrevTicker();
		this.newListingDate = dailyPriceJSON.getNewListingDate();
		this.tickerChangeDate = dailyPriceJSON.getTickerChangeDate();
		this.delistingDate = dailyPriceJSON.getDelistingDate();

		return this;
	}

	public boolean differentPrices(DailyPriceJSON p1) {
		return p1.getRegularMarketPrice() != this.getRegularMarketPrice() || p1.getRegularMarketOpen() != this.getRegularMarketOpen()
				|| p1.getRegularMarketDayHigh() != this.getRegularMarketDayHigh() || p1.getRegularMarketDayLow() != this.getRegularMarketDayLow()
				|| p1.getRegularMarketChangePercent() != this.getRegularMarketChangePercent();

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DailyPriceJSON that = (DailyPriceJSON) o;
		return Double.compare(getRegularMarketDayHigh(), that.getRegularMarketDayHigh()) == 0 && Double.compare(getRegularMarketDayLow(), that.getRegularMarketDayLow()) == 0 && Double.compare(getRegularMarketOpen(), that.getRegularMarketOpen()) == 0 && Double.compare(getRegularMarketPrice(), that.getRegularMarketPrice()) == 0 && Objects.equals(getSymbol(), that.getSymbol()) && Objects.equals(getDate(), that.getDate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSymbol(), getRegularMarketDayHigh(), getRegularMarketDayLow(), getRegularMarketOpen(), getDate(), getRegularMarketPrice());
	}

	@Override
	public String toString() {
		return "DailyPriceJSON{" +
				"symbol='" + symbol + '\'' +
				", date=" + date +
				", regularMarketOpen=" + regularMarketOpen +
				", regularMarketDayHigh=" + regularMarketDayHigh +
				", regularMarketDayLow=" + regularMarketDayLow +
				", regularMarketPrice=" + regularMarketPrice +
				", regularMarketChangePercent=" + regularMarketChangePercent +
				'}';
	}
}