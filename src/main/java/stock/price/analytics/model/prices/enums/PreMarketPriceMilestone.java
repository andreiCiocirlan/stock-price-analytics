package stock.price.analytics.model.prices.enums;

import java.util.List;

public enum PreMarketPriceMilestone implements PriceMiestone {
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
            return "Pre-Market: Up more than 10%";
        }
    },
    GAP_DOWN_10_PERCENT {
        @Override
        public String toString() {
            return "Pre-Market: Down more than 10%";
        }
    },
    GAP_UP_AND_GO {
        @Override
        public String toString() {
            return "Pre-Market: Gap UP & Go";
        }
    },
    GAP_DOWN_AND_GO {
        @Override
        public String toString() {
            return "Pre-Market: Gap Down & Go";
        }
    },
    KICKING_CANDLE_UP {
        @Override
        public String toString() {
            return "Pre-Market: Kicking candlestick UP";
        }
    },
    KICKING_CANDLE_DOWN {
        @Override
        public String toString() {
            return "Pre-Market: Kicking candlestick DOWN";
        }
    },
    NONE {
        @Override
        public String toString() {
            return "None";
        }
    };

    public static List<PriceMiestone> preMarketSchedulerValues() {
        return List.of(KICKING_CANDLE_UP, KICKING_CANDLE_DOWN, GAP_UP_AND_GO, GAP_DOWN_AND_GO);
    }
}