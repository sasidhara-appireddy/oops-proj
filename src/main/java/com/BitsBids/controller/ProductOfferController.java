package com.BitsBids.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BitsBids.dao.ProductDao;
import com.BitsBids.dao.ProductOfferDao;
import com.BitsBids.dao.UserDao;
import com.BitsBids.dto.CommanApiResponse;
import com.BitsBids.dto.ProductOfferRequestDto;
import com.BitsBids.model.Product;
import com.BitsBids.model.ProductOffer;
import com.BitsBids.model.User;
import com.BitsBids.utility.ResponseCode;
import com.BitsBids.utility.Constants.ProductOfferStatus;

@RestController
@RequestMapping("api/product/offer")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductOfferController {
	
	@Autowired
	private ProductOfferDao productOfferDao;
	
	@Autowired
	private ProductDao productDao;

	@Autowired
	private UserDao userDao;
	
	@PostMapping("add")
	public ResponseEntity<CommanApiResponse> addOffer(@RequestBody ProductOfferRequestDto request) {
		System.out.println("recieved request for ADD PRODUCT Offer");
		
		CommanApiResponse response = new CommanApiResponse();
		
		Product product = this.productDao.findById(request.getProductId()).get();
		User user =  this.userDao.findById(request.getUserId()).get();
		
		if(product.getPrice().compareTo(request.getAmount()) > 0) {
			response.setResponseMessage("Offer Amount can't be less than Product Min Amount!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
		}
		
		if(user.getWalletAmount().compareTo(request.getAmount()) < 0) {
			response.setResponseMessage("Insufficient Fund in Wallet to add the Offer!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
		}
		
		String requestTime = String
				.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		
		ProductOffer productOffer = new ProductOffer();
		productOffer.setAmount(request.getAmount());
		productOffer.setDateTime(requestTime);
		productOffer.setProduct(product);
		productOffer.setUser(user);
		productOffer.setStatus(ProductOfferStatus.PENDING.value());
		
		ProductOffer addedOffer = this.productOfferDao.save(productOffer);
		
		if(addedOffer == null) {
			response.setResponseMessage("Failed to add the offer!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		} else {
			response.setResponseMessage("Product Offer added succcessfully!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
		}
		
	}
	
	@GetMapping("fetch/product")
	public ResponseEntity<?> fetchProductOffersByProduct(@RequestParam("productId") int productId) {
		
		List<ProductOffer> offers = new ArrayList<>();
		
		Product product = this.productDao.findById(productId).get();
		
		offers = this.productOfferDao.findByProduct(product);

		return new ResponseEntity(offers, HttpStatus.OK);

	}
	
	@GetMapping("fetch/user")
	public ResponseEntity<?> fetchProductOffersByUser(@RequestParam("userId") int userId) {
		
		List<ProductOffer> offers = new ArrayList<>();
		
		User user = this.userDao.findById(userId).get();
		
		offers = this.productOfferDao.findByUser(user);

		return new ResponseEntity(offers, HttpStatus.OK);

	}
	
	@DeleteMapping("/id")
	public ResponseEntity<?> deleteOffer(@RequestParam("offerId") int offerId) {
		
		CommanApiResponse response = new CommanApiResponse();
		
		ProductOffer productOffer = this.productOfferDao.findById(offerId).get();
		
		this.productOfferDao.delete(productOffer);

		response.setResponseMessage("product offer deleted successful!!!");
		response.setResponseCode(ResponseCode.SUCCESS.value());

		return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);

	}
	

}
