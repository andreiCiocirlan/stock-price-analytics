package com.example.stockprices.model.quote;

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

	@JsonProperty("market")
	private String market;

	@JsonProperty("regularMarketVolume")
	private int regularMarketVolume;

	@JsonProperty("postMarketPrice")
	private double postMarketPrice;

	@JsonProperty("quoteSourceName")
	private String quoteSourceName;

	@JsonProperty("messageBoardId")
	private String messageBoardId;

	@JsonProperty("priceHint")
	private int priceHint;

	@JsonProperty("exchange")
	private String exchange;

	@JsonProperty("regularMarketDayLow")
	private double regularMarketDayLow;

	@JsonProperty("sourceInterval")
	private int sourceInterval;

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

	@JsonProperty("typeDisp")
	private String typeDisp;

	@JsonProperty("trailingPE")
	private double trailingPE;

	@JsonProperty("tradeable")
	private boolean tradeable;

	@JsonProperty("postMarketTime")
	private int postMarketTime;

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

	@JsonProperty("postMarketChangePercent")
	private double postMarketChangePercent;

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
	private int earningsTimestamp;

	@JsonProperty("ask")
	private double ask;

	@JsonProperty("epsTrailingTwelveMonths")
	private double epsTrailingTwelveMonths;

	@JsonProperty("bid")
	private double bid;

	@JsonProperty("triggerable")
	private boolean triggerable;

	@JsonProperty("priceToBook")
	private double priceToBook;

	@JsonProperty("longName")
	private String longName;

	@JsonProperty("earningsCallTimestampEnd")
	private int earningsCallTimestampEnd;

	@JsonProperty("earningsCallTimestampStart")
	private int earningsCallTimestampStart;

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
}