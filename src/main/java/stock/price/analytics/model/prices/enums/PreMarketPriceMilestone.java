package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

import java.util.List;

public enum PreMarketPriceMilestone implements PriceMilestone {
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
    GAP_UP_AND_GO {
        @Override
        public String toString() {
            return "Gap UP & Go";
        }
    },
    GAP_DOWN_AND_GO {
        @Override
        public String toString() {
            return "Gap Down & Go";
        }
    },
    KICKING_CANDLE_UP {
        @Override
        public String toString() {
            return "Kicking candlestick UP";
        }
    },
    KICKING_CANDLE_DOWN {
        @Override
        public String toString() {
            return "Kicking candlestick DOWN";
        }
    },
    PRE_NEW_4W_LOW {
        @Override
        public String toString() {
            return "New 4w Low";
        }
    },
    PRE_NEW_4W_HIGH {
        @Override
        public String toString() {
            return "New 4w High";
        }
    },
    PRE_NEW_52W_LOW {
        @Override
        public String toString() {
            return "New 52w Low";
        }
    },
    PRE_NEW_52W_HIGH {
        @Override
        public String toString() {
            return "New 52w High";
        }
    },
    PRE_NEW_ALL_TIME_LOW {
        @Override
        public String toString() {
            return "New All-time Low";
        }
    },
    PRE_NEW_ALL_TIME_HIGH {
        @Override
        public String toString() {
            return "New All-time High";
        }
    },
    NONE {
        @Override
        public String toString() {
            return "None";
        }
    };

    public static List<PriceMilestone> preMarketSchedulerValues() {
        return List.of(KICKING_CANDLE_UP, KICKING_CANDLE_DOWN, GAP_UP_AND_GO, GAP_DOWN_AND_GO, GAP_UP_10_PERCENT, GAP_DOWN_10_PERCENT, PRE_NEW_ALL_TIME_LOW, PRE_NEW_ALL_TIME_HIGH);
    }
}