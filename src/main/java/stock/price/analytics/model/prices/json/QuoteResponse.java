package stock.price.analytics.model.prices.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class QuoteResponse {

	@JsonProperty("result")
	private List<DailyPricesJSON> result;

	@JsonProperty("error")
	private Object error;

}