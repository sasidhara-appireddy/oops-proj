package com.BitsBids;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.BitsBids.dao.OrderDao;
import com.BitsBids.dao.ProductDao;
import com.BitsBids.dao.ProductOfferDao;
import com.BitsBids.dao.UserDao;
import com.BitsBids.model.Orders;
import com.BitsBids.model.Product;
import com.BitsBids.model.ProductOffer;
import com.BitsBids.model.User;
import com.BitsBids.service.EmailSenderService;
import com.BitsBids.utility.Helper;
import com.BitsBids.utility.Constants.DeliveryStatus;
import com.BitsBids.utility.Constants.DeliveryTime;
import com.BitsBids.utility.Constants.IsDeliveryAssigned;
import com.BitsBids.utility.Constants.OrderStatus;
import com.BitsBids.utility.Constants.ProductOfferStatus;
import com.BitsBids.utility.Constants.ProductSellStatus;

@Component
public class UpdateProductOrderTask {

	@Autowired
	private ProductDao productDao;

	@Autowired
	private ProductOfferDao productOfferDao;
	
	@Autowired
	private OrderDao orderDao;
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private EmailSenderService emailSenderService;

	@Scheduled(fixedRate = 60000) // 100,000 milliseconds = 5 minutes
	public void updateProductOffers() {
		
		System.out.println("PRODUCT ORDER UPDATE TASK START");

		LocalDateTime now = LocalDateTime.now();

		LocalDateTime todayMidnight = LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT);

		String startTime = String.valueOf(todayMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

		String endTime = String.valueOf(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

		List<Product> products = this.productDao.findByStatusInAndEndDateBetween(
				Arrays.asList(ProductSellStatus.AVAILABLE.value()), startTime, endTime);
		
		if(CollectionUtils.isEmpty(products)) {
			System.out.println("no available products found");
			
			System.out.println("PRODUCT ORDER UPDATE TASK END");
			return;
		}

		for (Product product : products) {

			List<ProductOffer> offers = this.productOfferDao.findByProduct(product);

			if (CollectionUtils.isEmpty(offers)) {

				product.setStatus(ProductSellStatus.UNSOLD.value());
				productDao.save(product);

				continue;
			}

			// Find the largest amount using Java Stream
			Optional<ProductOffer> maxOffer = offers.stream()
					.max((offer1, offer2) -> offer1.getAmount().compareTo(offer2.getAmount()));

			ProductOffer biggestOffer = maxOffer.get();

			biggestOffer = maxOffer.get();

			// updating product
			product.setStatus(ProductSellStatus.SOLD.value());
			product.setUser(maxOffer.get().getUser());
			productDao.save(product);
			
			System.out.println("Product Updated");

			for (ProductOffer offer : offers) {

				if (offer == biggestOffer) {
					// updating product offer entry
					offer.setStatus(ProductOfferStatus.WON.value());
					productOfferDao.save(offer);
				}

				else {
					// updating product offer entry
					offer.setStatus(ProductOfferStatus.LOSS.value());
					productOfferDao.save(offer);
				}
			}
			
			System.out.println("Product Offer Updated");
			
			// adding the order for the biggest offer
			String orderId = Helper.getAlphaNumericOrderId();
			
			LocalDateTime currentDateTime = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
			String formatDateTime = currentDateTime.format(formatter);
			
			Orders order = new Orders();
			order.setOrderId(orderId);
			order.setUser(biggestOffer.getUser());
			order.setProduct(product);
			order.setQuantity(product.getQuantity());
			order.setOrderDate(formatDateTime);
			order.setDeliveryDate(DeliveryStatus.PENDING.value());
			order.setDeliveryStatus(DeliveryStatus.PENDING.value());
			order.setDeliveryTime(DeliveryTime.DEFAULT.value());
			order.setDeliveryAssigned(IsDeliveryAssigned.NO.value());
			order.setProductOffer(biggestOffer);
			order.setStatus(OrderStatus.PENDING.value());

			orderDao.save(order);
			
			User customer = biggestOffer.getUser();
			customer.setWalletAmount(customer.getWalletAmount().subtract(biggestOffer.getAmount()));
			
			this.userDao.save(customer);
			
			String toEmail = customer.getEmailId();
			String subject = "Art Gallery - You have wont the product with our Offer";
			String message = "Hello "+customer.getFirstName()+",\r\n"
					+ "\r\n"
					+ "We are pleased to inform you that your bid for the following product has been successful:\r\n"
					+ "\r\n"
					+ "Product Name: "+product.getTitle()+"\r\n"
					+ "Description: "+product.getDescription()+"\r\n"
					+ "Quantity: "+product.getQuantity()+"\r\n"
					+ "Offer Amount: "+order.getProductOffer().getAmount()+"\r\n"
					+ "\r\n"
					+ "Your order is now confirmed, and we will process it shortly. Thank you for choosing our platform.\r\n"
					+ "\r\n"
					+ "If you have any questions or need further assistance, please don't hesitate to contact our support team.\r\n"
					+ "\r\n"
					+ "Best regards,\r\n"
					+ "Art Gallery";
			
			System.out.println(subject);
			System.out.println(message);

			try {
				this.emailSenderService.sendEmail(toEmail, subject, message);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
			System.out.println("Order Updated");

		}
		
		System.out.println("PRODUCT ORDER UPDATE TASK END");

	}

}
