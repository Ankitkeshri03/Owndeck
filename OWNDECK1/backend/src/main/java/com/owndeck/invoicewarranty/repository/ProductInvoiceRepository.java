package com.owndeck.invoicewarranty.repository;

import com.owndeck.invoicewarranty.entity.ProductInvoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductInvoiceRepository extends JpaRepository<ProductInvoice, Long> {

  List<ProductInvoice> findByUser_IdOrderByCreatedAtDesc(Long userId);

  List<ProductInvoice> findByUser_Id(Long userId);

  Optional<ProductInvoice> findByIdAndUser_Id(Long id, Long userId);
}
