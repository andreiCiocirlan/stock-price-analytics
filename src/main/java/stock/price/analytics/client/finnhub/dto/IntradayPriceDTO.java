package stock.price.analytics.client.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class IntradayPriceDTO {

    @JsonProperty("c")
    private double close;

    @JsonProperty("pc")
    private double previousClose;

    @JsonProperty("d")
    private double dailyChange;

    @JsonProperty("t")
    private long unixTimestamp;

    @JsonProperty("h")
    private double high;

    @JsonProperty("dp")
    private double percentChange;

    @JsonProperty("l")
    private double low;

    @JsonProperty("o")
    private double open;

}