package stock.price.analytics.repository.prices.gaps;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.gaps.PriceGap;

@Repository
public interface PriceGapsRepository extends JpaRepository<PriceGap, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            with closed_gaps as (
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'DAILY' AND pg.status = 'OPEN' AND pg.close between s.d_low and s.d_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'WEEKLY' AND pg.status = 'OPEN' AND pg.close between s.w_low and s.w_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'MONTHLY' AND pg.status = 'OPEN' AND pg.close between s.m_low and s.m_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'QUARTERLY' AND pg.status = 'OPEN' AND pg.close between s.q_low and s.q_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'YEARLY' AND pg.status = 'OPEN' AND pg.close between s.y_low and s.y_high)
            )
            UPDATE price_gaps
            set status = 'CLOSED'
            where id in (select id from closed_gaps)
            """, nativeQuery = true)
    int closePriceGaps();
}
