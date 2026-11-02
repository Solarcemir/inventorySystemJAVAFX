package com.inventory.backend.services;

import com.inventory.backend.exception.ApiException;
import com.inventory.backend.model.Product;
import com.inventory.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.RoundingMode;
import java.util.List;

 @Service
public class ProductService {

  private final ProductRepository repo;
  private final ActivityLogService logService;

  public ProductService(ProductRepository repo, ActivityLogService logService){
    this.repo = repo;
    this.logService = logService;
  }

  // Obtener solo productos activos (no eliminados)
  public List<Product> getAllProducts(){
    return repo.findByDeletedFalseOrDeletedIsNull();
  }

  // Obtener todos los productos incluyendo eliminados (para analytics)
  public List<Product> getAllProductsIncludingDeleted(){
    return repo.findAll();
  }

  @Transactional
  public Product createProduct(Product p){
    validate(p);
    p.setDeleted(false); // Asegurar que productos nuevos no estén eliminados
    Product saved = repo.save(p);
    logService.log("ADD_PRODUCT", "Added product \"" + saved.getProductName() + "\" (Qty: " + saved.getProductQuantity()
        + ", Price: $" + saved.getPrice().setScale(2, RoundingMode.HALF_UP) + ")");
    return saved;
  }

  // Soft delete - marcar como eliminado pero no borrar de la base de datos
  @Transactional
  public void deleteProductbyId(Long id){
    repo.findById(id).ifPresent(product -> {
      product.setDeleted(true);
      repo.save(product);
      logService.log("DELETE_PRODUCT", "Deleted product \"" + product.getProductName() + "\" (ID: " + id + ")");
    });
  }

  public Product getProductbyId(Long id){
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("Producto no encontrado por id"));
  }

  public Product getProductbyName(String name){
    return repo.findProductByProductName(name);
  }

  @Transactional
  public Product updateProduct(Long id, Product productDetails) {
    validate(productDetails);
    // search for the exisiting product
    Product product = getProductbyId(id);
    //update this product
    product.setProductName(productDetails.getProductName());
    product.setProvider(productDetails.getProvider());
    product.setCategory(productDetails.getCategory());
    product.setDescription(productDetails.getDescription());
    product.setCostPrice(productDetails.getCostPrice());
    product.setPrice(productDetails.getPrice());
    product.setProductQuantity(productDetails.getProductQuantity());
    product.setImagePath(productDetails.getImagePath());
    product.setLocation(productDetails.getLocation());
    Product saved = repo.save(product);
    logService.log("EDIT_PRODUCT", "Edited product \"" + saved.getProductName() + "\" (ID: " + id + ")");
    return saved;
  }

  /** A deleted product, or one without a price, cannot go into a sale or an invoice. */
  static void checkSellable(Product product){
    if(product.getDeleted()) {
      throw ApiException.conflict("El producto \"" + product.getProductName() + "\" fue eliminado");
    }
    if(product.getPrice() == null) {
      throw ApiException.conflict("El producto \"" + product.getProductName() + "\" no tiene precio");
    }
  }

  private void validate(Product p){
    if(p.getProductName() == null || p.getProductName().isBlank()) {
      throw ApiException.badRequest("El nombre del producto es obligatorio");
    }
    if(p.getPrice() == null || p.getPrice().signum() < 0) {
      throw ApiException.badRequest("El precio no puede ser negativo");
    }
    if(p.getCostPrice() != null && p.getCostPrice().signum() < 0) {
      throw ApiException.badRequest("El costo no puede ser negativo");
    }
    if(p.getProductQuantity() == null || p.getProductQuantity() < 0) {
      throw ApiException.badRequest("La cantidad no puede ser negativa");
    }
  }

  // Contar solo productos activos
  public long getProductCount(){
    return repo.countByDeletedFalseOrDeletedIsNull();
  }

  // Contar todos los productos incluyendo eliminados
  public long getTotalProductCount(){
    return repo.count();
  }
}
