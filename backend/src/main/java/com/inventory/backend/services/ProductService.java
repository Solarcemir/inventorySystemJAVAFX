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
  
  public List <Product> getAllProducts(){
    return repo.findAll();
  }

  public Product createProduct(Product p){
    if(p.getPrice() == null || p.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
       throw new IllegalArgumentException("El precio es negativo error en ProductService.java");
    }
    else {
      return repo.save(p);
    }
  }

  public void deleteProductbyId(Long id){
    if(repo.existsById(id))
    repo.deleteById(id);
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

   public long getProductCount(){
    return repo.count();
   }
}
