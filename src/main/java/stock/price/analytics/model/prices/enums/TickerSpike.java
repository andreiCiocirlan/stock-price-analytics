package stock.price.analytics.model.prices.enums;

import java.util.List;

public enum TickerSpike implements PriceMilestone {

    SPIKE_UP {
        @Override
        public String toString() {
            return "Spike Up";
        }
    },
    SPIKE_DOWN {
        @Override
        public String toString() {
            return "Spike Down";
        }
    },
    NONE {
        @Override
        public String toString() {
            return "None";
        }
    };

    public static List<PriceMilestone> tickerSpikes() {
        return List.of(SPIKE_UP, SPIKE_DOWN);
    }
}
