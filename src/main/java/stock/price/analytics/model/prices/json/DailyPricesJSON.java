package stock.price.analytics.model.prices.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stock.price.analytics.model.prices.PriceEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "daily_prices_json")
@JsonIgnoreProperties(ignoreUnknown=true)
public class DailyPricesJSON implements PriceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_prices_json_gen")
	@SequenceGenerator(name = "daily_prices_json_gen", sequenceName = "daily_prices_json_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("twoHundredDayAverageChangePercent")
	private double twoHundredDayAverageChangePercent;

	@JsonProperty("averageAnalystRating")
	private String averageAnalystRating;

	@JsonProperty("fiftyTwoWeekLowChangePercent")
	private double fiftyTwoWeekLowChangePercent;

	@JsonProperty("isEarningsDateEstimate")
	private boolean isEarningsDateEstimate;

	@JsonProperty("language")
	private String language;

	@JsonProperty("regularMarketDayRange")
	private String regularMarketDayRange;

	@JsonProperty("earningsTimestampEnd")
	private long earningsTimestampEnd;

	@JsonProperty("epsForward")
	private double epsForward;

	@JsonProperty("regularMarketDayHigh")
	private double regularMarketDayHigh;

	@JsonProperty("twoHundredDayAverageChange")
	private double twoHundredDayAverageChange;

	@JsonProperty("askSize")
	private long askSize;

	@JsonProperty("twoHundredDayAverage")
	private double twoHundredDayAverage;

	@JsonProperty("bookValue")
	private double bookValue;

	@JsonProperty("fiftyTwoWeekHighChange")
	private double fiftyTwoWeekHighChange;

	@JsonProperty("marketCap")
	private long marketCap;

	@JsonProperty("esgPopulated")
	private boolean esgPopulated;

	@JsonProperty("fiftyTwoWeekRange")
	private String fiftyTwoWeekRange;

	@JsonProperty("fiftyDayAverageChange")
	private double fiftyDayAverageChange;

	@JsonProperty("exchangeDataDelayedBy")
	private long exchangeDataDelayedBy;

	@JsonProperty("firstTradeDateMilliseconds")
	private long firstTradeDateMilliseconds;

	@JsonProperty("averageDailyVolume3Month")
	private long averageDailyVolume3Month;

	@JsonProperty("fiftyTwoWeekChangePercent")
	private double fiftyTwoWeekChangePercent;

	@JsonProperty("trailingAnnualDividendRate")
	private double trailingAnnualDividendRate;

	@JsonProperty("hasPrePostMarketData")
	private boolean hasPrePostMarketData;

	@JsonProperty("fiftyTwoWeekLow")
	private double fiftyTwoWeekLow;

	@JsonProperty("market")
	private String market;

	@JsonProperty("regularMarketVolume")
	private long regularMarketVolume;

	@JsonProperty("postMarketPrice")
	private double postMarketPrice;

	@JsonProperty("quoteSourceName")
	private String quoteSourceName;

	@JsonProperty("messageBoardId")
	private String messageBoardId;

	@JsonProperty("priceHint")
	private long priceHint;

	@JsonProperty("exchange")
	private String exchange;

	@JsonProperty("sourceInterval")
	private long sourceInterval;

	@JsonProperty("regularMarketDayLow")
	private double regularMarketDayLow;

	@JsonProperty("region")
	private String region;

	@JsonProperty("shortName")
	private String shortName;

	@JsonProperty("fiftyDayAverageChangePercent")
	private double fiftyDayAverageChangePercent;

	@JsonProperty("fullExchangeName")
	private String fullExchangeName;

	@JsonProperty("earningsTimestampStart")
	private long earningsTimestampStart;

	@JsonProperty("financialCurrency")
	private String financialCurrency;

	@JsonProperty("displayName")
	private String displayName;

	@JsonProperty("gmtOffSetMilliseconds")
	private long gmtOffSetMilliseconds;

	@JsonProperty("regularMarketOpen")
	private double regularMarketOpen;

	@Column(name = "date")
	@JsonProperty("regularMarketTime")
	private LocalDate date;

	@JsonProperty("regularMarketChangePercent")
	private double regularMarketChangePercent;

	@JsonProperty("quoteType")
	private String quoteType;

	@JsonProperty("trailingAnnualDividendYield")
	private double trailingAnnualDividendYield;

	@JsonProperty("averageDailyVolume10Day")
	private long averageDailyVolume10Day;

	@JsonProperty("fiftyTwoWeekLowChange")
	private double fiftyTwoWeekLowChange;

	@JsonProperty("fiftyTwoWeekHighChangePercent")
	private double fiftyTwoWeekHighChangePercent;

	@JsonProperty("earningsCallTimestampEnd")
	private long earningsCallTimestampEnd;

	@JsonProperty("typeDisp")
	private String typeDisp;

	@JsonProperty("trailingPE")
	private double trailingPE;

	@JsonProperty("tradeable")
	private boolean tradeable;

	@JsonProperty("postMarketTime")
	private long postMarketTime;

	@JsonProperty("currency")
	private String currency;

	@JsonProperty("sharesOutstanding")
	private long sharesOutstanding;

	@JsonProperty("regularMarketPreviousClose")
	private double regularMarketPreviousClose;

	@JsonProperty("fiftyTwoWeekHigh")
	private double fiftyTwoWeekHigh;

	@JsonProperty("exchangeTimezoneName")
	private String exchangeTimezoneName;

	@JsonProperty("postMarketChangePercent")
	private double postMarketChangePercent;

	@JsonProperty("regularMarketChange")
	private double regularMarketChange;

	@JsonProperty("bidSize")
	private long bidSize;

	@JsonProperty("priceEpsCurrentYear")
	private double priceEpsCurrentYear;

	@JsonProperty("cryptoTradeable")
	private boolean cryptoTradeable;

	@JsonProperty("fiftyDayAverage")
	private double fiftyDayAverage;

	@JsonProperty("exchangeTimezoneShortName")
	private String exchangeTimezoneShortName;

	@JsonProperty("epsCurrentYear")
	private double epsCurrentYear;

	@JsonProperty("customPriceAlertConfidence")
	private String customPriceAlertConfidence;

	@JsonProperty("regularMarketPrice")
	private double regularMarketPrice;

	@JsonProperty("marketState")
	private String marketState;

	@JsonProperty("postMarketChange")
	private double postMarketChange;

	@JsonProperty("forwardPE")
	private double forwardPE;

	@JsonProperty("earningsTimestamp")
	private long earningsTimestamp;

	@JsonProperty("ask")
	private double ask;

	@JsonProperty("epsTrailingTwelveMonths")
	private double epsTrailingTwelveMonths;

	@JsonProperty("bid")
	private double bid;

	@JsonProperty("triggerable")
	private boolean triggerable;

	@JsonProperty("earningsCallTimestampStart")
	private long earningsCallTimestampStart;

	@JsonProperty("priceToBook")
	private double priceToBook;

	@JsonProperty("longName")
	private String longName;

	@JsonProperty("dividendDate")
	private long dividendDate;

	@JsonProperty("dividendYield")
	private double dividendYield;

	@JsonProperty("dividendRate")
	private double dividendRate;

	@JsonProperty("prevName")
	private String prevName;

	@JsonProperty("prevExchange")
	private String prevExchange;

	@JsonProperty("exchangeTransferDate")
	private String exchangeTransferDate;

	@JsonProperty("preMarketTime")
	private LocalDate preMarketTime;

	@JsonProperty("preMarketPrice")
	private double preMarketPrice;

	@JsonProperty("preMarketChange")
	private double preMarketChange;

	@JsonProperty("preMarketChangePercent")
	private double preMarketChangePercent;

	@JsonProperty("nameChangeDate")
	private String nameChangeDate;

	@JsonProperty("ipoExpectedDate")
	private String ipoExpectedDate;

	@JsonProperty("openInterest")
	private long openInterest;

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

	public DailyPricesJSON updateFrom(DailyPricesJSON dailyPricesJSON) {
		this.symbol = dailyPricesJSON.getSymbol();
		this.twoHundredDayAverageChangePercent = dailyPricesJSON.getTwoHundredDayAverageChangePercent();
		this.averageAnalystRating = dailyPricesJSON.getAverageAnalystRating();
		this.fiftyTwoWeekLowChangePercent = dailyPricesJSON.getFiftyTwoWeekLowChangePercent();
		this.isEarningsDateEstimate = dailyPricesJSON.isEarningsDateEstimate();
		this.language = dailyPricesJSON.getLanguage();
		this.regularMarketDayRange = dailyPricesJSON.getRegularMarketDayRange();
		this.earningsTimestampEnd = dailyPricesJSON.getEarningsTimestampEnd();
		this.epsForward = dailyPricesJSON.getEpsForward();
		this.regularMarketDayHigh = dailyPricesJSON.getRegularMarketDayHigh();
		this.twoHundredDayAverageChange = dailyPricesJSON.getTwoHundredDayAverageChange();
		this.askSize = dailyPricesJSON.getAskSize();
		this.twoHundredDayAverage = dailyPricesJSON.getTwoHundredDayAverage();
		this.bookValue = dailyPricesJSON.getBookValue();
		this.fiftyTwoWeekHighChange = dailyPricesJSON.getFiftyTwoWeekHighChange();
		this.marketCap = dailyPricesJSON.getMarketCap();
		this.esgPopulated = dailyPricesJSON.isEsgPopulated();
		this.fiftyTwoWeekRange = dailyPricesJSON.getFiftyTwoWeekRange();
		this.fiftyDayAverageChange = dailyPricesJSON.getFiftyDayAverageChange();
		this.exchangeDataDelayedBy = dailyPricesJSON.getExchangeDataDelayedBy();
		this.firstTradeDateMilliseconds = dailyPricesJSON.getFirstTradeDateMilliseconds();
		this.averageDailyVolume3Month = dailyPricesJSON.getAverageDailyVolume3Month();
		this.fiftyTwoWeekChangePercent = dailyPricesJSON.getFiftyTwoWeekChangePercent();
		this.trailingAnnualDividendRate = dailyPricesJSON.getTrailingAnnualDividendRate();
		this.hasPrePostMarketData = dailyPricesJSON.isHasPrePostMarketData();
		this.fiftyTwoWeekLow = dailyPricesJSON.getFiftyTwoWeekLow();
		this.market = dailyPricesJSON.getMarket();
		this.regularMarketVolume = dailyPricesJSON.getRegularMarketVolume();
		this.postMarketPrice = dailyPricesJSON.getPostMarketPrice();
		this.quoteSourceName = dailyPricesJSON.getQuoteSourceName();
		this.messageBoardId = dailyPricesJSON.getMessageBoardId();
		this.priceHint = dailyPricesJSON.getPriceHint();
		this.exchange = dailyPricesJSON.getExchange();
		this.sourceInterval = dailyPricesJSON.getSourceInterval();
		this.regularMarketDayLow = dailyPricesJSON.getRegularMarketDayLow();
		this.region = dailyPricesJSON.getRegion();
		this.shortName = dailyPricesJSON.getShortName();
		this.fiftyDayAverageChangePercent = dailyPricesJSON.getFiftyDayAverageChangePercent();
		this.fullExchangeName = dailyPricesJSON.getFullExchangeName();
		this.earningsTimestampStart = dailyPricesJSON.getEarningsTimestampStart();
		this.financialCurrency = dailyPricesJSON.getFinancialCurrency();
		this.displayName = dailyPricesJSON.getDisplayName();
		this.gmtOffSetMilliseconds = dailyPricesJSON.getGmtOffSetMilliseconds();
		this.regularMarketOpen = dailyPricesJSON.getRegularMarketOpen();
		this.date = dailyPricesJSON.getDate();
		this.regularMarketChangePercent = dailyPricesJSON.getRegularMarketChangePercent();
		this.quoteType = dailyPricesJSON.getQuoteType();
		this.trailingAnnualDividendYield = dailyPricesJSON.getTrailingAnnualDividendYield();
		this.averageDailyVolume10Day = dailyPricesJSON.getAverageDailyVolume10Day();
		this.fiftyTwoWeekLowChange = dailyPricesJSON.getFiftyTwoWeekLowChange();
		this.fiftyTwoWeekHighChangePercent = dailyPricesJSON.getFiftyTwoWeekHighChangePercent();
		this.earningsCallTimestampEnd = dailyPricesJSON.getEarningsCallTimestampEnd();
		this.typeDisp = dailyPricesJSON.getTypeDisp();
		this.trailingPE = dailyPricesJSON.getTrailingPE();
		this.tradeable = dailyPricesJSON.isTradeable();
		this.postMarketTime = dailyPricesJSON.getPostMarketTime();
		this.currency = dailyPricesJSON.getCurrency();
		this.sharesOutstanding = dailyPricesJSON.getSharesOutstanding();
		this.regularMarketPreviousClose = dailyPricesJSON.getRegularMarketPreviousClose();
		this.fiftyTwoWeekHigh = dailyPricesJSON.getFiftyTwoWeekHigh();
		this.exchangeTimezoneName = dailyPricesJSON.getExchangeTimezoneName();
		this.postMarketChangePercent = dailyPricesJSON.getPostMarketChangePercent();
		this.regularMarketChange = dailyPricesJSON.getRegularMarketChange();
		this.bidSize = dailyPricesJSON.getBidSize();
		this.priceEpsCurrentYear = dailyPricesJSON.getPriceEpsCurrentYear();
		this.cryptoTradeable = dailyPricesJSON.isCryptoTradeable();
		this.fiftyDayAverage = dailyPricesJSON.getFiftyDayAverage();
		this.exchangeTimezoneShortName = dailyPricesJSON.getExchangeTimezoneShortName();
		this.epsCurrentYear = dailyPricesJSON.getEpsCurrentYear();
		this.customPriceAlertConfidence = dailyPricesJSON.getCustomPriceAlertConfidence();
		this.regularMarketPrice = dailyPricesJSON.getRegularMarketPrice();
		this.marketState = dailyPricesJSON.getMarketState();
		this.postMarketChange = dailyPricesJSON.getPostMarketChange();
		this.forwardPE = dailyPricesJSON.getForwardPE();
		this.earningsTimestamp = dailyPricesJSON.getEarningsTimestamp();
		this.ask = dailyPricesJSON.getAsk();
		this.epsTrailingTwelveMonths = dailyPricesJSON.getEpsTrailingTwelveMonths();
		this.bid = dailyPricesJSON.getBid();
		this.triggerable = dailyPricesJSON.isTriggerable();
		this.earningsCallTimestampStart = dailyPricesJSON.getEarningsCallTimestampStart();
		this.priceToBook = dailyPricesJSON.getPriceToBook();
		this.longName = dailyPricesJSON.getLongName();
		this.dividendDate = dailyPricesJSON.getDividendDate();
		this.dividendYield = dailyPricesJSON.getDividendYield();
		this.dividendRate = dailyPricesJSON.getDividendRate();
		this.prevName = dailyPricesJSON.getPrevName();
		this.prevExchange = dailyPricesJSON.getPrevExchange();
		this.exchangeTransferDate = dailyPricesJSON.getExchangeTransferDate();
		this.preMarketTime = dailyPricesJSON.getPreMarketTime();
		this.preMarketPrice = dailyPricesJSON.getPreMarketPrice();
		this.preMarketChange = dailyPricesJSON.getPreMarketChange();
		this.preMarketChangePercent = dailyPricesJSON.getPreMarketChangePercent();
		this.nameChangeDate = dailyPricesJSON.getNameChangeDate();
		this.ipoExpectedDate = dailyPricesJSON.getIpoExpectedDate();
		this.openInterest = dailyPricesJSON.getOpenInterest();
		this.prevTicker = dailyPricesJSON.getPrevTicker();
		this.newListingDate = dailyPricesJSON.getNewListingDate();
		this.tickerChangeDate = dailyPricesJSON.getTickerChangeDate();
		this.delistingDate = dailyPricesJSON.getDelistingDate();

		return this;
	}

	public boolean differentPrices(DailyPricesJSON p1) {
		return p1.getRegularMarketPrice() != this.getRegularMarketPrice() || p1.getRegularMarketOpen() != this.getRegularMarketOpen()
				|| p1.getRegularMarketDayHigh() != this.getRegularMarketDayHigh() || p1.getRegularMarketDayLow() != this.getRegularMarketDayLow()
				|| p1.getRegularMarketChangePercent() != this.getRegularMarketChangePercent();

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DailyPricesJSON that = (DailyPricesJSON) o;
		return Double.compare(getRegularMarketDayHigh(), that.getRegularMarketDayHigh()) == 0 && Double.compare(getRegularMarketDayLow(), that.getRegularMarketDayLow()) == 0 && Double.compare(getRegularMarketOpen(), that.getRegularMarketOpen()) == 0 && Double.compare(getRegularMarketPrice(), that.getRegularMarketPrice()) == 0 && Objects.equals(getSymbol(), that.getSymbol()) && Objects.equals(getDate(), that.getDate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSymbol(), getRegularMarketDayHigh(), getRegularMarketDayLow(), getRegularMarketOpen(), getDate(), getRegularMarketPrice());
	}

	@Override
	public String toString() {
		return "DailyPricesJSON{" +
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