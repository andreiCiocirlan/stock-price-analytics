package stock.price.analytics.model.prices.enums;

public enum PreMarketPriceMilestone {
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
    NONE {
        @Override
        public String toString() {
            return "None";
        }
    };
}