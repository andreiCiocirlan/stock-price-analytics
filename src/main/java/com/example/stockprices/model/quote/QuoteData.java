package com.example.stockprices.model.quote;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuoteData{

	@JsonProperty("quoteResponse")
	private QuoteResponse quoteResponse;
}