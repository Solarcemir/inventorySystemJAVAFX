package com.inventory.backend.controller;

import com.inventory.backend.model.*;
import com.inventory.backend.services.ProductService;

import com.inventory.backend.model.Product;

import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService ProductService;

  public ProductController(ProductService rep){
    ProductService = rep;
  }

  //entity for mapping for GET
  @GetMapping
  public List <Product> getAllProducts(){
    return ProductService.getAllProducts();
    //find all is spring crud method for SELECT *
  }


  @GetMapping("/{id}")
  public Product getById(@PathVariable Long id){
    return ProductService.getProductbyId(id);
  }

  //entinty for mapping for POST
  @PostMapping
  public Product createProduct(@RequestBody Product p) {
    return ProductService.createProduct(p);
  }
  
// modify
  @PutMapping("/{id}")
  public Product update(@PathVariable Long id,@RequestBody Product p){
    return ProductService.updateProduct(id, p);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id){
    ProductService.deleteProductbyId(id);
  }

  //return counts
  @GetMapping("/count")
  public long getProductCount(){
    return ProductService.getProductCount();
  }

}

