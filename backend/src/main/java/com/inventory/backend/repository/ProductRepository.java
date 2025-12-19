package com.inventory.backend.repository;

import com.inventory.backend.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends JpaRepository<Product,Long>{

   // this will be auto
   Product findProductByProductName(String productName);
}
