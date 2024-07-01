package com.example.stockprices.model.quote;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class QuoteResponse{

	@JsonProperty("result")
	private List<ResultItem> result;

	@JsonProperty("error")
	private Object error;
}