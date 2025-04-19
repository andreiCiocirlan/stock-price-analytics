package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

import java.util.List;

public enum IntradayPriceSpike implements PriceMilestone {

    INTRADAY_SPIKE_UP {
        @Override
        public String toString() {
            return "Intraday Spike Up";
        }
    },
    INTRADAY_SPIKE_DOWN {
        @Override
        public String toString() {
            return "Intraday Spike Down";
        }
    };

    public static List<PriceMilestone> intradaySpikes() {
        return List.of(INTRADAY_SPIKE_UP, INTRADAY_SPIKE_DOWN);
    }
}
