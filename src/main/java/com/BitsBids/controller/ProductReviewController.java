package com.BitsBids.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.BitsBids.dao.ProductReviewDao;
import com.BitsBids.dao.UserDao;
import com.BitsBids.dto.CommanApiResponse;
import com.BitsBids.model.ProductReview;
import com.BitsBids.model.User;
import com.BitsBids.utility.ResponseCode;

@RestController
@RequestMapping("api/product/review")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductReviewController {
	
	Logger LOG = LoggerFactory.getLogger(ProductReviewController.class);
	
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private ProductReviewDao reviewDao;
	
	@PostMapping("add")
	public ResponseEntity<?> register(@RequestBody ProductReview review) {
		LOG.info("Recieved request for Add product Review");

		CommanApiResponse response = new CommanApiResponse();

		if (review == null) {
			response.setResponseCode(ResponseCode.FAILED.value());
			response.setResponseMessage("Failed to add review");
			return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
		}
		
		User user = this.userDao.findById(review.getUserId()).get();
		review.setUser(user.getFirstName() + " " +user.getLastName());
		
		ProductReview addedReview = reviewDao.save(review);
		
		if (addedReview != null) {
			response.setResponseCode(ResponseCode.SUCCESS.value());
			response.setResponseMessage("product Review Added Successfully");
			return new ResponseEntity(response, HttpStatus.OK);
		}

		else {
			response.setResponseCode(ResponseCode.FAILED.value());
			response.setResponseMessage("Failed to add product review");
			return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GetMapping("fetch")
	public ResponseEntity<?> fetchProductReview(@RequestParam("productId") int productId) {
		LOG.info("Recieved request for Fetch Product Reviews for Hotel Id : "+productId);

		List<ProductReview> reviews = new ArrayList<>();
		reviews = reviewDao.findByProductId(productId);
		
		return new ResponseEntity(reviews, HttpStatus.OK);

	}
	
}
