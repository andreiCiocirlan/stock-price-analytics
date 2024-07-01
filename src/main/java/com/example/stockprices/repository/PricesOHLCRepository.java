package com.example.stockprices.repository;

import com.example.stockprices.model.prices.ohlc.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface PricesOHLCRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    @Query("SELECT p FROM DailyPriceOHLC p WHERE p.ticker = :ticker")
    List<DailyPriceOHLC> findDailyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM WeeklyPriceOHLC p WHERE p.ticker = :ticker")
    List<WeeklyPriceOHLC> findWeeklyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM MonthlyPriceOHLC p WHERE p.ticker = :ticker")
    List<MonthlyPriceOHLC> findMonthlyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM YearlyPriceOHLC p WHERE p.ticker = :ticker")
    List<YearlyPriceOHLC> findYearlyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM WeeklyPriceOHLC p WHERE p.startDate between :weekStart and :weekEnd")
    List<WeeklyPriceOHLC> findWeeklyOHLCBetween(LocalDate weekStart, LocalDate weekEnd);

    @Query("SELECT p FROM MonthlyPriceOHLC p WHERE p.startDate between :monthStart and :monthEnd")
    List<MonthlyPriceOHLC> findMonthlyOHLCBetween(LocalDate monthStart, LocalDate monthEnd);

    @Query("SELECT p FROM YearlyPriceOHLC p WHERE p.startDate between :yearStart and :yearEnd")
    List<YearlyPriceOHLC> findYearlyOHLCBetween(LocalDate yearStart, LocalDate yearEnd);

    @Query("SELECT p FROM WeeklyPriceOHLC p JOIN Stock s on s.ticker = p.ticker and s.xtbStock = true WHERE p.startDate between :weekStart and :weekEnd")
    List<WeeklyPriceOHLC> findWeeklyOHLC_XTBOnlyBetween(LocalDate weekStart, LocalDate weekEnd);

    @Query("SELECT p FROM MonthlyPriceOHLC p JOIN Stock s on s.ticker = p.ticker and s.xtbStock = true WHERE p.startDate between :monthStart and :monthEnd")
    List<MonthlyPriceOHLC> findMonthlyOHLC_XTBOnlyBetween(LocalDate monthStart, LocalDate monthEnd);

    @Query("SELECT p FROM YearlyPriceOHLC p JOIN Stock s on s.ticker = p.ticker and s.xtbStock = true WHERE p.startDate between :yearStart and :yearEnd")
    List<YearlyPriceOHLC> findYearlyOHLC_XTBOnlyBetween(LocalDate yearStart, LocalDate yearEnd);

}
