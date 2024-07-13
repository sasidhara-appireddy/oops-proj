package com.BitsBids.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BitsBids.dao.CategoryDao;
import com.BitsBids.dao.ProductDao;
import com.BitsBids.dao.UserDao;
import com.BitsBids.dto.CommanApiResponse;
import com.BitsBids.dto.ProductAddRequest;
import com.BitsBids.model.Category;
import com.BitsBids.model.Product;
import com.BitsBids.service.ProductService;
import com.BitsBids.utility.ResponseCode;
import com.BitsBids.utility.StorageService;
import com.BitsBids.utility.Constants.ProductActiveStatus;
import com.BitsBids.utility.Constants.ProductSellStatus;

@RestController
@RequestMapping("api/product")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private ProductDao productDao;
	
	@Autowired
	private CategoryDao categoryDao;
	
	@Autowired
	private StorageService storageService;
	
	@Autowired
	private UserDao userDao;
	
	@PostMapping("add")
	public ResponseEntity<?> addProduct(ProductAddRequest productDto) {
		System.out.println("recieved request for ADD PRODUCT");
		System.out.println(productDto);
		Product product=ProductAddRequest.toEntity(productDto);
		product.setStatus(ProductSellStatus.AVAILABLE.value());
		
		Optional<Category> optional = categoryDao.findById(productDto.getCategoryId());
		Category category = null;
		if(optional.isPresent()) {
			category = optional.get();
		}
		
		product.setActiveStatus(ProductActiveStatus.ACTIVE.value());
		product.setCategory(category);
		productService.addProduct(product, productDto.getImage());
		
		System.out.println("response sent!!!");
		return ResponseEntity.ok(product);
		
	}
	
	@PostMapping("update")
	public ResponseEntity<?> updateProduct(@RequestBody Product product) {
		System.out.println("recieved request for Update PRODUCT");
	
		CommanApiResponse response = new CommanApiResponse();
		
		this.productDao.save(product);
		
		response.setResponseMessage("Product Update Success");
		response.setResponseCode(ResponseCode.SUCCESS.value());
		
		return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
		
	}
	
	@GetMapping("all")
	public ResponseEntity<?> getAllProducts() {
		
		System.out.println("request came for getting all products");
		
		List<Product> products = new ArrayList<Product>();
		
		products = productDao.findByActiveStatus(ProductActiveStatus.ACTIVE.value());
		
		System.out.println("response sent!!!");
		
		return ResponseEntity.ok(products);
		
	}
	
	@GetMapping("id")
	public ResponseEntity<?> getProductById(@RequestParam("productId") int productId) {
		
		System.out.println("request came for getting Product by Product Id");
		
		Product product = new Product();
		
		Optional<Product> optional = productDao.findById(productId);
		
		if(optional.isPresent()) {
			product = optional.get();
		}
		System.out.println("response sent!!!");
		
		return ResponseEntity.ok(product);
		
	}
	
	@GetMapping("category")
	public ResponseEntity<?> getProductsByCategories(@RequestParam("categoryId") int categoryId) {
		
		System.out.println("request came for getting all products by category");
		
		List<Product> products = new ArrayList<Product>();
		
		products = productDao.findByCategoryIdAndActiveStatus(categoryId,ProductActiveStatus.ACTIVE.value());
		
		System.out.println("response sent!!!");
		
		return ResponseEntity.ok(products);
	}
	
	@GetMapping(value="/{productImageName}", produces = "image/*")
	public void fetchProductImage(@PathVariable("productImageName") String productImageName, HttpServletResponse resp) {
		System.out.println("request came for fetching product pic");
		System.out.println("Loading file: " + productImageName);
		Resource resource = storageService.load(productImageName);
		if(resource != null) {
			try(InputStream in = resource.getInputStream()) {
				ServletOutputStream out = resp.getOutputStream();
				FileCopyUtils.copy(in, out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("response sent!");
	}
	
	@GetMapping("delete")
	public ResponseEntity<?> deleteProduct(@RequestParam("productId") int productId) {
		
		CommanApiResponse response = new CommanApiResponse();
		
		Product product = this.productDao.findById(productId).get();
		product.setActiveStatus(ProductActiveStatus.INACTIVE.value());
		
		this.productDao.save(product);
		
		response.setResponseMessage("Product Deleted Success");
		response.setResponseCode(ResponseCode.SUCCESS.value());
		
		return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
		
	}

}
