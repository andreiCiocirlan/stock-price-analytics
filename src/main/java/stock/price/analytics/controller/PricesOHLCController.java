package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.controller.dto.CandleOHLCWithDate;
import stock.price.analytics.repository.prices.PricesOHLCRepository;

import java.util.List;

@RequestMapping("/ohlc")
@RestController
@Slf4j
@RequiredArgsConstructor
public class PricesOHLCController {

    private final PricesOHLCRepository pricesOhlcRepository;

    @GetMapping("/daily")
    public List<CandleOHLCWithDate> dailyOHLC(@RequestParam("ticker") String ticker) {
        return pricesOhlcRepository.findDailyOHLCByTicker(ticker)
                .stream()
                .map(dp -> new CandleOHLCWithDate(dp.getDate(), dp.getOpen(), dp.getHigh(), dp.getLow(), dp.getClose()))
                .toList();
    }

    @GetMapping("/weekly")
    public List<CandleOHLCWithDate> weeklyOHLC(@RequestParam("ticker") String ticker) {
        return pricesOhlcRepository.findWeeklyOHLCByTicker(ticker)
                .stream()
                .map(dp -> new CandleOHLCWithDate(dp.getStartDate(), dp.getOpen(), dp.getHigh(), dp.getLow(), dp.getClose()))
//                .sorted(Comparator.comparing(CandleOHLCWithDate::date))
                .toList();
    }

    @GetMapping("/monthly")
    public List<CandleOHLCWithDate> monthlyOHLC(@RequestParam("ticker") String ticker) {
        return pricesOhlcRepository.findMonthlyOHLCByTicker(ticker)
                .stream()
                .map(dp -> new CandleOHLCWithDate(dp.getStartDate(), dp.getOpen(), dp.getHigh(), dp.getLow(), dp.getClose()))
//                .sorted(Comparator.comparing(CandleOHLCWithDate::date))
                .toList();
    }

    @GetMapping("/yearly")
    public List<CandleOHLCWithDate> yearlyOHLC(@RequestParam("ticker") String ticker) {
        return pricesOhlcRepository.findYearlyOHLCByTicker(ticker)
                .stream()
                .map(dp -> new CandleOHLCWithDate(dp.getStartDate(), dp.getOpen(), dp.getHigh(), dp.getLow(), dp.getClose()))
//                .sorted(Comparator.comparing(CandleOHLCWithDate::date))
                .toList();
    }

}
