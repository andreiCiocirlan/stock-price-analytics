package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.StockPriceMilestone;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.util.List;

public enum PreMarketGap implements StockPriceMilestone<DailyPrice> {

    GAP_UP {
        @Override
        public String toString() {
            return "Gap Up";
        }
    },
    GAP_DOWN {
        @Override
        public String toString() {
            return "Gap Down";
        }
    },
    GAP_UP_10_PERCENT {
        @Override
        public String toString() {
            return "Gap-Up more than 10%";
        }
    },
    GAP_DOWN_10_PERCENT {
        @Override
        public String toString() {
            return "Gap-Down more than 10%";
        }
    },
    GAP_UP_5_PERCENT {
        @Override
        public String toString() {
            return "Gap-Up more than 5%";
        }
    },
    GAP_DOWN_5_PERCENT {
        @Override
        public String toString() {
            return "Gap-Down more than 5%";
        }
    };

    public static List<PriceMilestone> gap_5_10_percent_values() {
        return List.of(GAP_DOWN_5_PERCENT, GAP_UP_5_PERCENT, GAP_DOWN_10_PERCENT, GAP_UP_10_PERCENT);
    }

    @Override
    public boolean isMetFor(DailyPrice context) {
        return switch (this) {
            case GAP_UP -> context.getPerformance() > 0;
            case GAP_DOWN -> context.getPerformance() < 0;
            case GAP_UP_10_PERCENT -> context.getPerformance() > 10d;
            case GAP_DOWN_10_PERCENT -> context.getPerformance() < -10d;
            case GAP_UP_5_PERCENT -> context.getPerformance() > 5d;
            case GAP_DOWN_5_PERCENT -> context.getPerformance() < -5d;
        };
    }
}