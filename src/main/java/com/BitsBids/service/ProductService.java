package com.BitsBids.service;

import org.springframework.web.multipart.MultipartFile;

import com.BitsBids.model.Product;

public interface ProductService {
	
	void addProduct(Product product, MultipartFile productImmage);

}
