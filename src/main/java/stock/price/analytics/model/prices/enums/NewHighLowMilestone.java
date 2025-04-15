package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

public enum NewHighLowMilestone implements PriceMilestone {

    NEW_52W_HIGH("new-high-low"),
    NEW_4W_HIGH("new-high-low"),
    NEW_ALL_TIME_HIGH("new-high-low"),
    NEW_52W_LOW("new-high-low"),
    NEW_4W_LOW("new-high-low"),
    NEW_ALL_TIME_LOW("new-high-low");

    private final String type;

    NewHighLowMilestone(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return this.type;
    }
}
