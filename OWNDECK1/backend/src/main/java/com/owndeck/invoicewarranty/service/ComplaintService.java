package com.owndeck.invoicewarranty.service;

import com.owndeck.invoicewarranty.dto.ai.ComplaintResponse;
import com.owndeck.invoicewarranty.entity.ProductInvoice;
import com.owndeck.invoicewarranty.exception.NotFoundException;
import com.owndeck.invoicewarranty.repository.ProductInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintService {

  private final ProductInvoiceRepository productRepository;
  private final OpenAiService openAiService;

  public ComplaintService(ProductInvoiceRepository productRepository, OpenAiService openAiService) {
    this.productRepository = productRepository;
    this.openAiService = openAiService;
  }

  @Transactional(readOnly = true)
  public ComplaintResponse generateComplaint(Long userId, Long productId, String issueDescription) {
    ProductInvoice product = productRepository.findByIdAndUser_Id(productId, userId)
        .orElseThrow(() -> new NotFoundException("Product not found."));
    return openAiService.generateComplaint(product, issueDescription);
  }
}
