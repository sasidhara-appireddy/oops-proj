package com.BitsBids.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BitsBids.model.Product;
import com.BitsBids.model.ProductOffer;
import com.BitsBids.model.User;

@Repository
public interface ProductOfferDao extends JpaRepository<ProductOffer, Integer> {
	
	List<ProductOffer> findByProduct(Product product);
	List<ProductOffer> findByUser(User user);

}
