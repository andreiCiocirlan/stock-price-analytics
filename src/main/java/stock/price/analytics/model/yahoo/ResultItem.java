package stock.price.analytics.model.yahoo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResultItem{

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("dividendDate")
	private int dividendDate;

	@JsonProperty("twoHundredDayAverageChangePercent")
	private double twoHundredDayAverageChangePercent;

	@JsonProperty("fiftyTwoWeekLowChangePercent")
	private double fiftyTwoWeekLowChangePercent;

	@JsonProperty("isEarningsDateEstimate")
	private boolean isEarningsDateEstimate;

	@JsonProperty("averageAnalystRating")
	private String averageAnalystRating;

	@JsonProperty("language")
	private String language;

	@JsonProperty("regularMarketDayRange")
	private String regularMarketDayRange;

	@JsonProperty("earningsTimestampEnd")
	private int earningsTimestampEnd;

	@JsonProperty("epsForward")
	private double epsForward;

	@JsonProperty("regularMarketDayHigh")
	private double regularMarketDayHigh;

	@JsonProperty("twoHundredDayAverageChange")
	private double twoHundredDayAverageChange;

	@JsonProperty("askSize")
	private int askSize;

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

	@JsonProperty("firstTradeDateMilliseconds")
	private long firstTradeDateMilliseconds;

	@JsonProperty("averageDailyVolume3Month")
	private int averageDailyVolume3Month;

	@JsonProperty("exchangeDataDelayedBy")
	private int exchangeDataDelayedBy;

	@JsonProperty("fiftyTwoWeekChangePercent")
	private double fiftyTwoWeekChangePercent;

	@JsonProperty("trailingAnnualDividendRate")
	private double trailingAnnualDividendRate;

	@JsonProperty("hasPrePostMarketData")
	private boolean hasPrePostMarketData;

	@JsonProperty("fiftyTwoWeekLow")
	private double fiftyTwoWeekLow;

	@JsonProperty("regularMarketVolume")
	private int regularMarketVolume;

	@JsonProperty("market")
	private String market;

	@JsonProperty("quoteSourceName")
	private String quoteSourceName;

	@JsonProperty("messageBoardId")
	private String messageBoardId;

	@JsonProperty("priceHint")
	private int priceHint;

	@JsonProperty("regularMarketDayLow")
	private double regularMarketDayLow;

	@JsonProperty("sourceInterval")
	private int sourceInterval;

	@JsonProperty("exchange")
	private String exchange;

	@JsonProperty("region")
	private String region;

	@JsonProperty("shortName")
	private String shortName;

	@JsonProperty("fiftyDayAverageChangePercent")
	private double fiftyDayAverageChangePercent;

	@JsonProperty("fullExchangeName")
	private String fullExchangeName;

	@JsonProperty("earningsTimestampStart")
	private int earningsTimestampStart;

	@JsonProperty("financialCurrency")
	private String financialCurrency;

	@JsonProperty("displayName")
	private String displayName;

	@JsonProperty("gmtOffSetMilliseconds")
	private int gmtOffSetMilliseconds;

	@JsonProperty("regularMarketOpen")
	private double regularMarketOpen;

	@JsonProperty("regularMarketTime")
	private int regularMarketTime;

	@JsonProperty("regularMarketChangePercent")
	private double regularMarketChangePercent;

	@JsonProperty("quoteType")
	private String quoteType;

	@JsonProperty("trailingAnnualDividendYield")
	private double trailingAnnualDividendYield;

	@JsonProperty("averageDailyVolume10Day")
	private int averageDailyVolume10Day;

	@JsonProperty("fiftyTwoWeekLowChange")
	private double fiftyTwoWeekLowChange;

	@JsonProperty("fiftyTwoWeekHighChangePercent")
	private double fiftyTwoWeekHighChangePercent;

	@JsonProperty("earningsCallTimestampEnd")
	private int earningsCallTimestampEnd;

	@JsonProperty("typeDisp")
	private String typeDisp;

	@JsonProperty("trailingPE")
	private double trailingPE;

	@JsonProperty("tradeable")
	private boolean tradeable;

	@JsonProperty("currency")
	private String currency;

	@JsonProperty("sharesOutstanding")
	private int sharesOutstanding;

	@JsonProperty("regularMarketPreviousClose")
	private double regularMarketPreviousClose;

	@JsonProperty("fiftyTwoWeekHigh")
	private double fiftyTwoWeekHigh;

	@JsonProperty("exchangeTimezoneName")
	private String exchangeTimezoneName;

	@JsonProperty("regularMarketChange")
	private double regularMarketChange;

	@JsonProperty("bidSize")
	private int bidSize;

	@JsonProperty("priceEpsCurrentYear")
	private double priceEpsCurrentYear;

	@JsonProperty("cryptoTradeable")
	private boolean cryptoTradeable;

	@JsonProperty("fiftyDayAverage")
	private double fiftyDayAverage;

	@JsonProperty("epsCurrentYear")
	private double epsCurrentYear;

	@JsonProperty("exchangeTimezoneShortName")
	private String exchangeTimezoneShortName;

	@JsonProperty("customPriceAlertConfidence")
	private String customPriceAlertConfidence;

	@JsonProperty("regularMarketPrice")
	private double regularMarketPrice;

	@JsonProperty("marketState")
	private String marketState;

	@JsonProperty("forwardPE")
	private double forwardPE;

	@JsonProperty("earningsTimestamp")
	private int earningsTimestamp;

	@JsonProperty("ask")
	private double ask;

	@JsonProperty("epsTrailingTwelveMonths")
	private double epsTrailingTwelveMonths;

	@JsonProperty("bid")
	private double bid;

	@JsonProperty("triggerable")
	private boolean triggerable;

	@JsonProperty("earningsCallTimestampStart")
	private int earningsCallTimestampStart;

	@JsonProperty("priceToBook")
	private double priceToBook;

	@JsonProperty("longName")
	private String longName;

	@JsonProperty("dividendYield")
	private double dividendYield;

	@JsonProperty("dividendRate")
	private double dividendRate;

	@JsonProperty("ipoExpectedDate")
	private String ipoExpectedDate;

	@JsonProperty("prevName")
	private String prevName;

	@JsonProperty("nameChangeDate")
	private String nameChangeDate;

	public String getSymbol(){
		return symbol;
	}

	public int getDividendDate(){
		return dividendDate;
	}

	public double getTwoHundredDayAverageChangePercent(){
		return twoHundredDayAverageChangePercent;
	}

	public double getFiftyTwoWeekLowChangePercent(){
		return fiftyTwoWeekLowChangePercent;
	}

	public boolean isIsEarningsDateEstimate(){
		return isEarningsDateEstimate;
	}

	public String getAverageAnalystRating(){
		return averageAnalystRating;
	}

	public String getLanguage(){
		return language;
	}

	public String getRegularMarketDayRange(){
		return regularMarketDayRange;
	}

	public int getEarningsTimestampEnd(){
		return earningsTimestampEnd;
	}

	public double getEpsForward(){
		return epsForward;
	}

	public double getRegularMarketDayHigh(){
		return regularMarketDayHigh;
	}

	public double getTwoHundredDayAverageChange(){
		return twoHundredDayAverageChange;
	}

	public int getAskSize(){
		return askSize;
	}

	public double getTwoHundredDayAverage(){
		return twoHundredDayAverage;
	}

	public double getBookValue(){
		return bookValue;
	}

	public double getFiftyTwoWeekHighChange(){
		return fiftyTwoWeekHighChange;
	}

	public long getMarketCap(){
		return marketCap;
	}

	public boolean isEsgPopulated(){
		return esgPopulated;
	}

	public String getFiftyTwoWeekRange(){
		return fiftyTwoWeekRange;
	}

	public double getFiftyDayAverageChange(){
		return fiftyDayAverageChange;
	}

	public long getFirstTradeDateMilliseconds(){
		return firstTradeDateMilliseconds;
	}

	public int getAverageDailyVolume3Month(){
		return averageDailyVolume3Month;
	}

	public int getExchangeDataDelayedBy(){
		return exchangeDataDelayedBy;
	}

	public double getFiftyTwoWeekChangePercent(){
		return fiftyTwoWeekChangePercent;
	}

	public double getTrailingAnnualDividendRate(){
		return trailingAnnualDividendRate;
	}

	public boolean isHasPrePostMarketData(){
		return hasPrePostMarketData;
	}

	public double getFiftyTwoWeekLow(){
		return fiftyTwoWeekLow;
	}

	public int getRegularMarketVolume(){
		return regularMarketVolume;
	}

	public String getMarket(){
		return market;
	}

	public String getQuoteSourceName(){
		return quoteSourceName;
	}

	public String getMessageBoardId(){
		return messageBoardId;
	}

	public int getPriceHint(){
		return priceHint;
	}

	public double getRegularMarketDayLow(){
		return regularMarketDayLow;
	}

	public int getSourceInterval(){
		return sourceInterval;
	}

	public String getExchange(){
		return exchange;
	}

	public String getRegion(){
		return region;
	}

	public String getShortName(){
		return shortName;
	}

	public double getFiftyDayAverageChangePercent(){
		return fiftyDayAverageChangePercent;
	}

	public String getFullExchangeName(){
		return fullExchangeName;
	}

	public int getEarningsTimestampStart(){
		return earningsTimestampStart;
	}

	public String getFinancialCurrency(){
		return financialCurrency;
	}

	public String getDisplayName(){
		return displayName;
	}

	public int getGmtOffSetMilliseconds(){
		return gmtOffSetMilliseconds;
	}

	public double getRegularMarketOpen(){
		return regularMarketOpen;
	}

	public int getRegularMarketTime(){
		return regularMarketTime;
	}

	public double getRegularMarketChangePercent(){
		return regularMarketChangePercent;
	}

	public String getQuoteType(){
		return quoteType;
	}

	public double getTrailingAnnualDividendYield(){
		return trailingAnnualDividendYield;
	}

	public int getAverageDailyVolume10Day(){
		return averageDailyVolume10Day;
	}

	public double getFiftyTwoWeekLowChange(){
		return fiftyTwoWeekLowChange;
	}

	public double getFiftyTwoWeekHighChangePercent(){
		return fiftyTwoWeekHighChangePercent;
	}

	public int getEarningsCallTimestampEnd(){
		return earningsCallTimestampEnd;
	}

	public String getTypeDisp(){
		return typeDisp;
	}

	public double getTrailingPE(){
		return trailingPE;
	}

	public boolean isTradeable(){
		return tradeable;
	}

	public String getCurrency(){
		return currency;
	}

	public int getSharesOutstanding(){
		return sharesOutstanding;
	}

	public double getRegularMarketPreviousClose(){
		return regularMarketPreviousClose;
	}

	public double getFiftyTwoWeekHigh(){
		return fiftyTwoWeekHigh;
	}

	public String getExchangeTimezoneName(){
		return exchangeTimezoneName;
	}

	public double getRegularMarketChange(){
		return regularMarketChange;
	}

	public int getBidSize(){
		return bidSize;
	}

	public double getPriceEpsCurrentYear(){
		return priceEpsCurrentYear;
	}

	public boolean isCryptoTradeable(){
		return cryptoTradeable;
	}

	public double getFiftyDayAverage(){
		return fiftyDayAverage;
	}

	public double getEpsCurrentYear(){
		return epsCurrentYear;
	}

	public String getExchangeTimezoneShortName(){
		return exchangeTimezoneShortName;
	}

	public String getCustomPriceAlertConfidence(){
		return customPriceAlertConfidence;
	}

	public double getRegularMarketPrice(){
		return regularMarketPrice;
	}

	public String getMarketState(){
		return marketState;
	}

	public double getForwardPE(){
		return forwardPE;
	}

	public int getEarningsTimestamp(){
		return earningsTimestamp;
	}

	public double getAsk(){
		return ask;
	}

	public double getEpsTrailingTwelveMonths(){
		return epsTrailingTwelveMonths;
	}

	public double getBid(){
		return bid;
	}

	public boolean isTriggerable(){
		return triggerable;
	}

	public int getEarningsCallTimestampStart(){
		return earningsCallTimestampStart;
	}

	public double getPriceToBook(){
		return priceToBook;
	}

	public String getLongName(){
		return longName;
	}

	public double getDividendYield(){
		return dividendYield;
	}

	public double getDividendRate(){
		return dividendRate;
	}

	public String getIpoExpectedDate(){
		return ipoExpectedDate;
	}

	public String getPrevName(){
		return prevName;
	}

	public String getNameChangeDate(){
		return nameChangeDate;
	}
}