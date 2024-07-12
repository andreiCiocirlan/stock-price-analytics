package stock.price.analytics.model.yahoo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YFinanceQuoteResponse{

	@JsonProperty("quoteResponse")
	private QuoteResponse quoteResponse;

	public QuoteResponse getQuoteResponse(){
		return quoteResponse;
	}
}