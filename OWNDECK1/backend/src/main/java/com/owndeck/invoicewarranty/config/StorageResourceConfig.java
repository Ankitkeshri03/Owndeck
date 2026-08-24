package com.owndeck.invoicewarranty.config;

import com.owndeck.invoicewarranty.util.ProjectPathResolver;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StorageResourceConfig implements WebMvcConfigurer {

  private final String storageLocation;

  public StorageResourceConfig(@Value("${app.storage.location}") String storageLocation) {
    this.storageLocation = storageLocation;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path uploadPath = ProjectPathResolver.resolveBackendPath(storageLocation);
    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(uploadPath.toUri().toString() + "/");
  }
}
