package stock.price.analytics.old_code.prev52w;

public class StockServiceOld {

    /**

     in stockrepository
     List<Stock> findByXtbStockTrueAndDelistedDateNullAndTickerIn(Set<String> tickers);



    public List<Stock> xtbStocksAndNotDelistedFrom(Set<String> tickers) {
        return stockRepository.findByXtbStockTrueAndDelistedDateNullAndTickerIn(tickers);
    }

    public Map<HighLowPeriod, List<String>> updateStocksHighLow(Map<String, List<Previous52WPrices>> previous52wByTicker) {
        Map<String, List<Double>> highLowByTicker4w = highLowsForPeriod(previous52wByTicker, HighLowPeriod.HIGH_LOW_4W);
        Map<String, List<Double>> highLowByTicker52w = highLowsForPeriod(previous52wByTicker, HighLowPeriod.HIGH_LOW_52W);

        Set<String> tickersImported = previous52wByTicker.keySet();
        List<Stock> stocks = xtbStocksAndNotDelistedFrom(tickersImported);
        Map<String, Stock> stocksMap = stocks.stream().collect(Collectors.toMap(Stock::getTicker, s -> s));
        Map<HighLowPeriod, List<String>> highLowToUpdatedMap = new HashMap<>();

        for (Map.Entry<String, List<Previous52WPrices>> entry : previous52wByTicker.entrySet()) {
            String ticker = entry.getKey();
            Stock stock = stocksMap.get(ticker);

            double high4w = highLowByTicker4w.get(ticker).getFirst();
            double high52w = highLowByTicker52w.get(ticker).getFirst();
            updateHighs(stock, high4w, high52w, ticker, highLowToUpdatedMap);

            double low4w = highLowByTicker4w.get(ticker).getLast();
            double low52w = highLowByTicker52w.get(ticker).getLast();
            updateLows(stock, low4w, low52w, ticker, highLowToUpdatedMap);
        }

        return highLowToUpdatedMap;
    }

    private void updateHighs(Stock stock, double high4w, double high52w, String ticker, Map<HighLowPeriod, List<String>> highLowToUpdatedMap) {
        if (high4w > stock.getHighest()) { // surpassed highest today (update all periods)
            stock.setHighest(high4w);
            stock.setHigh4w(high4w);
            stock.setHigh52w(high4w);
            addAllToUpdatedMap(ticker, highLowToUpdatedMap);
        } else {
            if (high4w != stock.getHigh4w()) { // update needed (new 4w high today OR period surpassed and 2nd high taken)
                stock.setHigh4w(high4w);
                addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_4W);
            }
            if (high52w != stock.getHigh52w()) { // update needed (new 52w high today OR period surpassed and 2nd high taken)
                stock.setHigh52w(high52w);
                addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_52W);
            }
        }
    }

    private void updateLows(Stock stock, double low4w, double low52w, String ticker,
                            Map<HighLowPeriod, List<String>> highLowToUpdatedMap) {
        if (low4w < stock.getLowest()) { // surpassed lowest today (update all periods)
            stock.setLowest(low4w);
            stock.setLow4w(low4w);
            stock.setLow52w(low4w);
            addAllToUpdatedMap(ticker, highLowToUpdatedMap);
        } else {
            if (low4w != stock.getLow4w()) { // update needed (new 4w low today OR period surpassed and 2nd low taken)
                stock.setLow4w(low4w);
                addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_4W);
            }

            if (low52w != stock.getLow52w()) { // update needed (new 52w low today OR period surpassed and 2nd low taken)
                stock.setLow52w(low52w);
                addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_52W);
            }
        }
    }

    private void addAllToUpdatedMap(String ticker, Map<HighLowPeriod, List<String>> highLowToUpdatedMap) {
        addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_4W);
        addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_52W);
        addToUpdatedMap(ticker, highLowToUpdatedMap, HighLowPeriod.HIGH_LOW_ALL_TIME);
    }

    private void addToUpdatedMap(String ticker, Map<HighLowPeriod, List<String>> highLowToUpdatedMap, HighLowPeriod period) {
        highLowToUpdatedMap.computeIfAbsent(period, _ -> new ArrayList<>()).add(ticker);
    }

    public Map<String, List<Double>> highLowsForPeriod(Map<String, List<Previous52WPrices>> previousPricesByTicker, HighLowPeriod highLowPeriod) {
        return previousPricesByTicker.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // ticker
                        entry -> {
                            List<Previous52WPrices> prices = entry.getValue();
                            return switch (highLowPeriod) {
                                case HIGH_LOW_4W -> Arrays.asList(
                                        prices.stream().limit(4).mapToDouble(Previous52WPrices::getHigh).max().orElse(Double.NaN),
                                        prices.stream().limit(4).mapToDouble(Previous52WPrices::getLow).min().orElse(Double.NaN)
                                );
                                case HIGH_LOW_52W -> Arrays.asList(
                                        prices.stream().mapToDouble(Previous52WPrices::getHigh).max().orElse(Double.NaN),
                                        prices.stream().mapToDouble(Previous52WPrices::getLow).min().orElse(Double.NaN)
                                );
                                case HIGH_LOW_ALL_TIME ->
                                        throw new IllegalArgumentException("HIGH_LOW_ALL_TIME is not supported.");
                            };
                        }
                ));
    }

     */
}
