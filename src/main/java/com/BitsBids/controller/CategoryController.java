package com.BitsBids.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BitsBids.dao.CategoryDao;
import com.BitsBids.dao.ProductDao;
import com.BitsBids.dto.CommanApiResponse;
import com.BitsBids.model.Category;
import com.BitsBids.model.Product;
import com.BitsBids.utility.ResponseCode;
import com.BitsBids.utility.Constants.ProductActiveStatus;

@RestController
@RequestMapping("api/category")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {

	@Autowired
	private CategoryDao categoryDao;
	
	@Autowired
	private ProductDao productDao;

	@GetMapping("all")
	public ResponseEntity<List<Category>> getAllCategories() {
		
        System.out.println("request came for getting all categories");
		
		List<Category> categories = this.categoryDao.findByStatus(ProductActiveStatus.ACTIVE.value());
		
		ResponseEntity<List<Category>> response = new ResponseEntity<>(categories, HttpStatus.OK);
		
		System.out.println("response sent");
		
		return response;
		
	}
	
	@PostMapping("add")
	public ResponseEntity add(@RequestBody Category category) {
		
		System.out.println("request came for add category");
		
		category.setStatus(ProductActiveStatus.ACTIVE.value());
		Category c = categoryDao.save(category);
		
		if(c != null) {
			System.out.println("response sent");
			return new ResponseEntity( c ,HttpStatus.OK);
		}
		
		else {
			System.out.println("response sent");
			return new ResponseEntity("Failed to add category!",HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	@GetMapping("delete")
	public ResponseEntity<?> deleteCategory(@RequestParam("categoryId") int categoryId) {
		
		CommanApiResponse response = new CommanApiResponse();
		
		Category category = this.categoryDao.findById(categoryId).get();
		category.setStatus(ProductActiveStatus.INACTIVE.value());
		
		this.categoryDao.save(category);
		
		List<Product> products = this.productDao.findByCategoryId(categoryId);
		
		for(Product product : products) {
			product.setActiveStatus(ProductActiveStatus.INACTIVE.value());
			
			this.productDao.save(product);
		}
		
		response.setResponseMessage("Category Deleted Success");
		response.setResponseCode(ResponseCode.SUCCESS.value());
		
		return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
		
	}
	
	@PostMapping("update")
	public ResponseEntity<?> updateCategory(@RequestBody Category category) {
		
		CommanApiResponse response = new CommanApiResponse();
		
		this.categoryDao.save(category);
		
		response.setResponseMessage("Category Updated Success");
		response.setResponseCode(ResponseCode.SUCCESS.value());
		
		return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
		
	}
	
}

