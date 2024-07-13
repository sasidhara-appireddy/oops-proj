package com.BitsBids.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BitsBids.dao.CartDao;
import com.BitsBids.dao.OrderDao;
import com.BitsBids.dao.ProductDao;
import com.BitsBids.dao.UserDao;
import com.BitsBids.dto.CommanApiResponse;
import com.BitsBids.dto.MyOrderResponse;
import com.BitsBids.dto.OrderDataResponse;
import com.BitsBids.dto.UpdateDeliveryStatusRequest;
import com.BitsBids.model.Cart;
import com.BitsBids.model.Orders;
import com.BitsBids.model.Product;
import com.BitsBids.model.User;
import com.BitsBids.service.EmailSenderService;
import com.BitsBids.utility.Helper;
import com.BitsBids.utility.ResponseCode;
import com.BitsBids.utility.Constants.DeliveryStatus;
import com.BitsBids.utility.Constants.DeliveryTime;
import com.BitsBids.utility.Constants.IsDeliveryAssigned;
import com.BitsBids.utility.Constants.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("api/user/")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class OrderController {

	@Autowired
	private OrderDao orderDao;

	@Autowired
	private CartDao cartDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private ProductDao productDao;
 
	@Autowired
	private EmailSenderService emailSenderService;
	
	private ObjectMapper objectMapper = new ObjectMapper();

	@PostMapping("order")
	public ResponseEntity customerOrder(@RequestParam("userId") int userId) throws JsonProcessingException {

		System.out.println("request came for ORDER FOR CUSTOMER ID : " + userId);

		String orderId = Helper.getAlphaNumericOrderId();

		List<Cart> userCarts = cartDao.findByUser_id(userId);

		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
		String formatDateTime = currentDateTime.format(formatter);

		for (Cart cart : userCarts) {

			Orders order = new Orders();
			order.setOrderId(orderId);
			order.setUser(cart.getUser());
			order.setProduct(cart.getProduct());
			order.setQuantity(cart.getQuantity());
			order.setOrderDate(formatDateTime);
			order.setDeliveryDate(DeliveryStatus.PENDING.value());
			order.setDeliveryStatus(DeliveryStatus.PENDING.value());
			order.setDeliveryTime(DeliveryTime.DEFAULT.value());
			order.setDeliveryAssigned(IsDeliveryAssigned.NO.value());

			orderDao.save(order);
			
			Product updatedProduct = cart.getProduct();
			updatedProduct.setQuantity(updatedProduct.getQuantity() - cart.getQuantity());
			
			productDao.save(updatedProduct);
			
			cartDao.delete(cart);
		}

		System.out.println("response sent!!!");

		return new ResponseEntity("ORDER SUCCESS", HttpStatus.OK);

	}

	@GetMapping("myorder")
	public ResponseEntity getMyOrder(@RequestParam("userId") int userId) throws JsonProcessingException {

		System.out.println("request came for MY ORDER for USER ID : " + userId);

		List<Orders> userOrder = orderDao.findByUser_id(userId);

		OrderDataResponse orderResponse = new OrderDataResponse();

		List<MyOrderResponse> orderDatas = new ArrayList<>();

		for (Orders order : userOrder) {
			MyOrderResponse orderData = new MyOrderResponse();
			orderData.setId(order.getId());
			orderData.setOrderId(order.getOrderId());
			orderData.setProductDescription(order.getProduct().getDescription());
			orderData.setProductName(order.getProduct().getTitle());
			orderData.setProductImage(order.getProduct().getImageName());
			orderData.setQuantity(order.getQuantity());
			orderData.setOrderDate(order.getOrderDate());
			orderData.setProductId(order.getProduct().getId());
			orderData.setDeliveryDate(order.getDeliveryDate() + " " + order.getDeliveryTime());
			orderData.setDeliveryStatus(order.getDeliveryStatus());
			orderData.setProductOffer(order.getProductOffer());
			orderData.setTotalPrice(String.valueOf(order.getProductOffer().getAmount()));
			orderData.setStatus(order.getStatus());
			if (order.getDeliveryPersonId() == 0) {
				orderData.setDeliveryPersonContact(DeliveryStatus.PENDING.value());
				orderData.setDeliveryPersonName(DeliveryStatus.PENDING.value());
			}

			else {

				User deliveryPerson = null;

				Optional<User> optionalDeliveryPerson = this.userDao.findById(order.getDeliveryPersonId());

				deliveryPerson = optionalDeliveryPerson.get();

				orderData.setDeliveryPersonContact(deliveryPerson.getPhoneNo());
				orderData.setDeliveryPersonName(deliveryPerson.getFirstName());
			}
			orderDatas.add(orderData);
		}

		String json = objectMapper.writeValueAsString(orderDatas);

		System.out.println(json);

		return new ResponseEntity(orderDatas, HttpStatus.OK);

	}

	@GetMapping("admin/allorder")
	public ResponseEntity getAllOrder() throws JsonProcessingException {

		System.out.println("request came for FETCH ALL ORDERS");

		List<Orders> userOrder = orderDao.findAll();

		OrderDataResponse orderResponse = new OrderDataResponse();

		List<MyOrderResponse> orderDatas = new ArrayList<>();

		for (Orders order : userOrder) {
			MyOrderResponse orderData = new MyOrderResponse();
			orderData.setId(order.getId());
			orderData.setOrderId(order.getOrderId());
			orderData.setProductDescription(order.getProduct().getDescription());
			orderData.setProductName(order.getProduct().getTitle());
			orderData.setProductImage(order.getProduct().getImageName());
			orderData.setQuantity(order.getQuantity());
			orderData.setOrderDate(order.getOrderDate());
			orderData.setProductId(order.getProduct().getId());
			orderData.setDeliveryDate(order.getDeliveryDate() + " " + order.getDeliveryTime());
			orderData.setDeliveryStatus(order.getDeliveryStatus());
			orderData.setTotalPrice(String.valueOf(order.getProductOffer().getAmount()));

			orderData.setUserId(order.getUser().getId());
			orderData.setUserName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
			orderData.setUserPhone(order.getUser().getPhoneNo());
			orderData.setAddress(order.getUser().getAddress());
			orderData.setProductOffer(order.getProductOffer());
			orderData.setStatus(order.getStatus());
			if (order.getDeliveryPersonId() == 0) {
				orderData.setDeliveryPersonContact(DeliveryStatus.PENDING.value());
				orderData.setDeliveryPersonName(DeliveryStatus.PENDING.value());
			}

			else {
				User deliveryPerson = null;

				Optional<User> optionalDeliveryPerson = this.userDao.findById(order.getDeliveryPersonId());

				deliveryPerson = optionalDeliveryPerson.get();

				orderData.setDeliveryPersonContact(deliveryPerson.getPhoneNo());
				orderData.setDeliveryPersonName(deliveryPerson.getFirstName());
			}
			orderDatas.add(orderData);

		}

		
		System.out.println("response sent !!!");

		return new ResponseEntity(orderDatas, HttpStatus.OK);

	}

	@GetMapping("admin/showorder")
	public ResponseEntity getOrdersByOrderId(@RequestParam("orderId") String orderId) throws JsonProcessingException {

		System.out.println("request came for FETCH ORDERS BY ORDER ID : " + orderId);

		List<Orders> userOrder = orderDao.findByOrderId(orderId);

		List<MyOrderResponse> orderDatas = new ArrayList<>();

		for (Orders order : userOrder) {
			MyOrderResponse orderData = new MyOrderResponse();
			orderData.setId(order.getId());
			orderData.setOrderId(order.getOrderId());
			orderData.setProductDescription(order.getProduct().getDescription());
			orderData.setProductName(order.getProduct().getTitle());
			orderData.setProductImage(order.getProduct().getImageName());
			orderData.setQuantity(order.getQuantity());
			orderData.setOrderDate(order.getOrderDate());
			orderData.setProductId(order.getProduct().getId());
			orderData.setDeliveryDate(order.getDeliveryDate() + " " + order.getDeliveryTime());
			orderData.setDeliveryStatus(order.getDeliveryStatus());
			orderData.setTotalPrice(String.valueOf(order.getProductOffer().getAmount()));

			orderData.setUserId(order.getUser().getId());
			orderData.setUserName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
			orderData.setUserPhone(order.getUser().getPhoneNo());
			orderData.setAddress(order.getUser().getAddress());
			orderData.setProductOffer(order.getProductOffer());
			orderData.setStatus(order.getStatus());
			if (order.getDeliveryPersonId() == 0) {
				orderData.setDeliveryPersonContact(DeliveryStatus.PENDING.value());
				orderData.setDeliveryPersonName(DeliveryStatus.PENDING.value());
			}

			else {
				User deliveryPerson = null;

				Optional<User> optionalDeliveryPerson = this.userDao.findById(order.getDeliveryPersonId());

				deliveryPerson = optionalDeliveryPerson.get();

				orderData.setDeliveryPersonContact(deliveryPerson.getPhoneNo());
				orderData.setDeliveryPersonName(deliveryPerson.getFirstName());
			}
			orderDatas.add(orderData);

		}

		String json = objectMapper.writeValueAsString(orderDatas);

		System.out.println(json);

		System.out.println("response sent !!!");

		return new ResponseEntity(orderDatas, HttpStatus.OK);

	}

	@PostMapping("admin/order/deliveryStatus/update")
	public ResponseEntity updateOrderDeliveryStatus(@RequestBody UpdateDeliveryStatusRequest deliveryRequest)
			throws JsonProcessingException {

		System.out.println("response came for UPDATE DELIVERY STATUS");

		System.out.println(deliveryRequest);

		List<Orders> orders = orderDao.findByOrderId(deliveryRequest.getOrderId());

		for (Orders order : orders) {
			order.setDeliveryDate(deliveryRequest.getDeliveryDate());
			order.setDeliveryStatus(deliveryRequest.getDeliveryStatus());
			order.setDeliveryTime(deliveryRequest.getDeliveryTime());
			
			if(deliveryRequest.getDeliveryStatus().equals(DeliveryStatus.DELIVERED.value())) {
				order.setStatus(OrderStatus.RECEIVED.value());
			}
			
			orderDao.save(order);
		}

		List<Orders> userOrder = orderDao.findByOrderId(deliveryRequest.getOrderId());

		List<MyOrderResponse> orderDatas = new ArrayList<>();

		for (Orders order : userOrder) {
			MyOrderResponse orderData = new MyOrderResponse();
			orderData.setId(order.getId());
			orderData.setOrderId(order.getOrderId());
			orderData.setProductDescription(order.getProduct().getDescription());
			orderData.setProductName(order.getProduct().getTitle());
			orderData.setProductImage(order.getProduct().getImageName());
			orderData.setQuantity(order.getQuantity());
			orderData.setOrderDate(order.getOrderDate());
			orderData.setProductId(order.getProduct().getId());
			orderData.setDeliveryDate(order.getDeliveryDate() + " " + order.getDeliveryTime());
			orderData.setDeliveryStatus(order.getDeliveryStatus());
			orderData.setTotalPrice(String.valueOf(order.getProductOffer().getAmount()));

			orderData.setUserId(order.getUser().getId());
			orderData.setUserName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
			orderData.setUserPhone(order.getUser().getPhoneNo());
			orderData.setAddress(order.getUser().getAddress());
			orderData.setProductOffer(order.getProductOffer());
			orderData.setStatus(order.getStatus());
			
			if (order.getDeliveryPersonId() == 0) {
				orderData.setDeliveryPersonContact(DeliveryStatus.PENDING.value());
				orderData.setDeliveryPersonName(DeliveryStatus.PENDING.value());
			}

			else {
				User deliveryPerson = null;

				Optional<User> optionalDeliveryPerson = this.userDao.findById(order.getDeliveryPersonId());

				deliveryPerson = optionalDeliveryPerson.get();

				orderData.setDeliveryPersonContact(deliveryPerson.getPhoneNo());
				orderData.setDeliveryPersonName(deliveryPerson.getFirstName());
			}
			orderDatas.add(orderData);

		}

		String orderJson = objectMapper.writeValueAsString(orderDatas);

		System.out.println(orderJson);

		System.out.println("response sent !!!");

		return new ResponseEntity(orderDatas, HttpStatus.OK);
	}

	@PostMapping("admin/order/assignDelivery")
	public ResponseEntity assignDeliveryPersonForOrder(@RequestBody UpdateDeliveryStatusRequest deliveryRequest)
			throws JsonProcessingException {

		System.out.println("response came for ASSIGN DELIVERY PERSON FPOR ORDERS");

		System.out.println(deliveryRequest);

		List<Orders> orders = orderDao.findByOrderId(deliveryRequest.getOrderId());

		User deliveryPerson = null;

		Optional<User> optionalDeliveryPerson = this.userDao.findById(deliveryRequest.getDeliveryId());

		if (optionalDeliveryPerson.isPresent()) {
			deliveryPerson = optionalDeliveryPerson.get();
		}

		for (Orders order : orders) {
			order.setDeliveryAssigned(IsDeliveryAssigned.YES.value());
			order.setDeliveryPersonId(deliveryRequest.getDeliveryId());
			orderDao.save(order);
		}

		List<Orders> userOrder = orderDao.findByOrderId(deliveryRequest.getOrderId());

		List<MyOrderResponse> orderDatas = new ArrayList<>();

		for (Orders order : userOrder) {
			MyOrderResponse orderData = new MyOrderResponse();
			orderData.setId(order.getId());
			orderData.setOrderId(order.getOrderId());
			orderData.setProductDescription(order.getProduct().getDescription());
			orderData.setProductName(order.getProduct().getTitle());
			orderData.setProductImage(order.getProduct().getImageName());
			orderData.setQuantity(order.getQuantity());
			orderData.setOrderDate(order.getOrderDate());
			orderData.setProductId(order.getProduct().getId());
			orderData.setDeliveryDate(order.getDeliveryDate() + " " + order.getDeliveryTime());
			orderData.setDeliveryStatus(order.getDeliveryStatus());
			orderData.setTotalPrice(String.valueOf(order.getProductOffer().getAmount()));

			orderData.setUserId(order.getUser().getId());
			orderData.setUserName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
			orderData.setUserPhone(order.getUser().getPhoneNo());
			orderData.setAddress(order.getUser().getAddress());
			orderData.setProductOffer(order.getProductOffer());
			orderData.setStatus(order.getStatus());
			
			if (order.getDeliveryPersonId() == 0) {
				orderData.setDeliveryPersonContact(DeliveryStatus.PENDING.value());
				orderData.setDeliveryPersonName(DeliveryStatus.PENDING.value());
			}

			else {
				User dPerson = null;

				Optional<User> optionalPerson = this.userDao.findById(order.getDeliveryPersonId());

				dPerson = optionalPerson.get();

				orderData.setDeliveryPersonContact(dPerson.getPhoneNo());
				orderData.setDeliveryPersonName(dPerson.getFirstName());
			}

			orderDatas.add(orderData);

		}

		String orderJson = objectMapper.writeValueAsString(orderDatas);

		System.out.println(orderJson);

		System.out.println("response sent !!!");

		return new ResponseEntity(orderDatas, HttpStatus.OK);
	}

	@GetMapping("delivery/myorder")
	public ResponseEntity getMyDeliveryOrders(@RequestParam("deliveryPersonId") int deliveryPersonId)
			throws JsonProcessingException {

		System.out.println("request came for MY DELIVERY ORDER for USER ID : " + deliveryPersonId);

		User person = null;

		Optional<User> oD = this.userDao.findById(deliveryPersonId);

		if (oD.isPresent()) {
			person = oD.get();
		}

		List<Orders> userOrder = orderDao.findByDeliveryPersonId(deliveryPersonId);

		List<MyOrderResponse> orderDatas = new ArrayList<>();

		for (Orders order : userOrder) {
			MyOrderResponse orderData = new MyOrderResponse();
			orderData.setId(order.getId());
			orderData.setOrderId(order.getOrderId());
			orderData.setProductDescription(order.getProduct().getDescription());
			orderData.setProductName(order.getProduct().getTitle());
			orderData.setProductImage(order.getProduct().getImageName());
			orderData.setQuantity(order.getQuantity());
			orderData.setOrderDate(order.getOrderDate());
			orderData.setProductId(order.getProduct().getId());
			orderData.setDeliveryDate(order.getDeliveryDate() + " " + order.getDeliveryTime());
			orderData.setDeliveryStatus(order.getDeliveryStatus());
			orderData.setTotalPrice(String.valueOf(order.getProductOffer().getAmount()));

			orderData.setUserId(order.getUser().getId());
			orderData.setUserName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
			orderData.setUserPhone(order.getUser().getPhoneNo());
			orderData.setAddress(order.getUser().getAddress());
			orderData.setProductOffer(order.getProductOffer());
			orderData.setStatus(order.getStatus());
			
			if (order.getDeliveryPersonId() == 0) {
				orderData.setDeliveryPersonContact(DeliveryStatus.PENDING.value());
				orderData.setDeliveryPersonName(DeliveryStatus.PENDING.value());
			}

			else {
				orderData.setDeliveryPersonContact(person.getPhoneNo());
				orderData.setDeliveryPersonName(person.getFirstName());
			}

			orderDatas.add(orderData);

		}

		String json = objectMapper.writeValueAsString(orderDatas);

		System.out.println(json);

		return new ResponseEntity(orderDatas, HttpStatus.OK);

	}
	
	@GetMapping("order/today/totalAmount")
	public ResponseEntity getTodaysTotalAmount()  {

		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String formatDateTime = currentDateTime.format(formatter);
		
		System.out.println(formatDateTime);
		
		List<Orders> todaysOrder = orderDao.findByOrderDateContainingIgnoreCase(formatDateTime);
		
		if(CollectionUtils.isEmpty(todaysOrder)) {
			return new ResponseEntity("0.00", HttpStatus.OK);
		}
		
		BigDecimal totalAmount = BigDecimal.ZERO;

		for (Orders order : todaysOrder) {
			
			Product product = order.getProduct();
			
			BigDecimal productPrice = product.getPrice()
					.multiply(BigDecimal.valueOf(order.getQuantity()));
			
			totalAmount = totalAmount.add(productPrice);
			
		}

		System.out.println("Today Total Order Amount : "+totalAmount);

		return new ResponseEntity(String.valueOf(totalAmount), HttpStatus.OK);

	}
	
	@GetMapping("order/return")
	public ResponseEntity returnOrder(@RequestParam("orderId") int orderId)  {
		
		CommanApiResponse response = new CommanApiResponse();
        
		Orders order = this.orderDao.findById(orderId).get();
		
		long currentTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		
		long orderTime = Long.parseLong(order.getProduct().getEndDate());
		
		// Calculate the time difference in milliseconds
        long timeDifference = currentTime - orderTime;

        // Calculate the equivalent of 7 days in milliseconds
        long sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L;

        if (timeDifference > sevenDaysInMillis) {
            
        	response.setResponseMessage("Order cancellation is unavailable; 7-day limit exceeded.");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
        	
        } else {
            order.setStatus(OrderStatus.RETURN.value());
            this.orderDao.save(order);
            
            User user = order.getUser();
            user.setWalletAmount(user.getWalletAmount().add(order.getProductOffer().getAmount()));
            
            this.userDao.save(user);
            
            response.setResponseMessage("Returns Accepted");
			response.setResponseCode(ResponseCode.SUCCESS.value());
			
			String toEmail = user.getEmailId();
			String subject = "Art Gallery - Order Return Accepted";
			String message = "Hello "+user.getFirstName()+",\n"
					+ "\n"
					+ "We wanted to inform you that your request for returning the following order has been accepted:\n"
					+ "\n"
					+ "Order ID: ["+order.getOrderId()+"]\n"
					+ "\n"
					+ "Your return request has been successfully processed, and we will initiate the return process accordingly.\n"
					+ "\n"
					+ "If you have any questions or need further assistance, please feel free to contact our support team.\n"
					+ "\n"
					+ "Thank you for choosing our services.\n"
					+ "\n"
					+ "Best regards,\n"
					+ "Art Gallery";

			System.out.println(subject);
			System.out.println(message);
			
			try {
				this.emailSenderService.sendEmail(toEmail, subject, message);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
        }
		
		
	}
	
	@GetMapping("order/cancel")
	public ResponseEntity cancelOrder(@RequestParam("orderId") int orderId)  {
		
		CommanApiResponse response = new CommanApiResponse();
        
		Orders order = this.orderDao.findById(orderId).get();


        if (order.getStatus().equals(OrderStatus.RECEIVED.value())) {
            
        	response.setResponseMessage("Delivered orders cannot be canceled; request a return.");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
        	
        } else {
            order.setStatus(OrderStatus.CANCEL.value());
            order.setDeliveryStatus(DeliveryStatus.CANCELLED.value());
            this.orderDao.save(order);
            
            User user = order.getUser();
            user.setWalletAmount(user.getWalletAmount().add(order.getProductOffer().getAmount()));
            
            this.userDao.save(user);
            
            String toEmail = user.getEmailId();
			String subject = "Art Gallery - Order Cancelled Successful";
			String message = "Hello "+user.getFirstName()+",\n"
					+ "\n"
					+ "We are writing to inform you that your order has been successfully canceled:\n"
					+ "\n"
					+ "Order ID: ["+order.getOrderId()+"]\n"
					+ "\n"
					+ "Your order cancellation request has been processed, and the necessary refunds, if applicable, will be initiated shortly.\n"
					+ "\n"
					+ "If you have any questions or need further assistance, please don't hesitate to contact our customer support team. We're here to help!\n"
					+ "\n"
					+ "Thank you for choosing our services.\n"
					+ "\n"
					+ "Best regards,\n"
					+ "Art Gallery";

			System.out.println(subject);
			System.out.println(message);
            
			try {
				this.emailSenderService.sendEmail(toEmail, subject, message);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
            response.setResponseMessage("Order Cancelled");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
        }
	
	}

}
