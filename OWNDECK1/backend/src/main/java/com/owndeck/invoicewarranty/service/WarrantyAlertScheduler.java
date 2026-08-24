package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.repository.ProductInvoiceRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WarrantyAlertScheduler {

  private static final Logger log = LoggerFactory.getLogger(WarrantyAlertScheduler.class);

  private final ProductInvoiceRepository productRepository;

  public WarrantyAlertScheduler(ProductInvoiceRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Scheduled(cron = "${app.alert.cron:0 0 9 * * *}")
  @Transactional(readOnly = true)
  public void scanWarrantyExpiries() {
    LocalDate today = LocalDate.now();
    long oneMonthAlerts = 0;
    long oneDayAlerts = 0;

    for (var product : productRepository.findAll()) {
      long days = ChronoUnit.DAYS.between(today, product.getExpiryDate());
      if (days == 30) {
        oneMonthAlerts++;
      } else if (days == 1) {
        oneDayAlerts++;
      }
    }

    if (oneMonthAlerts > 0 || oneDayAlerts > 0) {
      log.info("Warranty alert scan found {} items expiring in 30 days and {} items expiring in 1 day for in-app reminders.",
          oneMonthAlerts, oneDayAlerts);
    }
  }
}
