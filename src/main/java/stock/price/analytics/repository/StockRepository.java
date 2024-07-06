package stock.price.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.stocks.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
}
