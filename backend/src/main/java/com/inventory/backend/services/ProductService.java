package com.inventory.backend.services;

import com.inventory.backend.model.Product;
import com.inventory.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;

import javax.management.RuntimeErrorException;

 @Service
public class ProductService {
  
  private final ProductRepository repo;

  public ProductService(ProductRepository repo){
    this.repo = repo;
  }
  
  // Obtener solo productos activos (no eliminados)
  public List<Product> getAllProducts(){
    return repo.findByDeletedFalseOrDeletedIsNull();
  }
  
  // Obtener todos los productos incluyendo eliminados (para analytics)
  public List<Product> getAllProductsIncludingDeleted(){
    return repo.findAll();
  }

  public Product createProduct(Product p){
    if(p.getPrice() == null || p.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
       throw new IllegalArgumentException("El precio es negativo error en ProductService.java");
    }
    p.setDeleted(false); // Asegurar que productos nuevos no estén eliminados
    return repo.save(p);
  }

  // Soft delete - marcar como eliminado pero no borrar de la base de datos
  public void deleteProductbyId(Long id){
    if(repo.existsById(id)) {
      Product product = repo.findById(id).orElse(null);
      if (product != null) {
        product.setDeleted(true);
        repo.save(product);
      }
    }
  }

  public Product getProductbyId(Long id){
    return repo.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado por id"));
  }

  public Product getProductbyName(String name){
    return repo.findProductByProductName(name);
  }

  public Product updateProduct(Long id, Product productDetails) {
    
    // search for the exisiting product
    Product product = getProductbyId(id);    
    //update this product
    product.setProductName(productDetails.getProductName());
    product.setProvider(productDetails.getProvider());
    product.setPrice(productDetails.getPrice());
    product.setProductQuantity(productDetails.getProductQuantity());
    product.setDescription(productDetails.getDescription());
    return repo.save(product);
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
