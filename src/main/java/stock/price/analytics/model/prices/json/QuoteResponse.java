package stock.price.analytics.model.prices.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuoteResponse {

	@JsonProperty("result")
	private List<DailyPricesJSON> result;

	@JsonProperty("error")
	private Object error;

}