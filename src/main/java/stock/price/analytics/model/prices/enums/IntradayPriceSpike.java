package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

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

}
