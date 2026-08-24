package com.owndeck.invoicewarranty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class InvoiceWarrantyApplication {

  public static void main(String[] args) {
    SpringApplication.run(InvoiceWarrantyApplication.class, args);
  }
}
