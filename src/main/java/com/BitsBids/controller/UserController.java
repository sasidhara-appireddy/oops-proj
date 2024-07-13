package com.BitsBids.controller;

import java.math.BigDecimal;
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

import com.BitsBids.dao.AddressDao;
import com.BitsBids.dao.OtpVerificationDao;
import com.BitsBids.dao.UserDao;
import com.BitsBids.dto.AddUserRequest;
import com.BitsBids.dto.AddWalletMoneyRequestDto;
import com.BitsBids.dto.CommanApiResponse;
import com.BitsBids.dto.UserLoginRequest;
import com.BitsBids.dto.UserRegistrationResponse;
import com.BitsBids.dto.UserVerifyRegisterRequest;
import com.BitsBids.model.Address;
import com.BitsBids.model.OtpVerification;
import com.BitsBids.model.User;
import com.BitsBids.service.EmailSenderService;
import com.BitsBids.utility.Helper;
import com.BitsBids.utility.ResponseCode;

@RestController
@RequestMapping("api/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

	@Autowired
	private UserDao userDao;

	@Autowired
	private AddressDao addressDao;

	@Autowired
	private OtpVerificationDao otpVerificationDao;

	@Autowired
	private EmailSenderService emailSenderService;

	@PostMapping("register")
	public ResponseEntity<?> registerUser(@RequestBody AddUserRequest userRequest) {
		System.out.println("recieved request for REGISTER USER");
		System.out.println(userRequest);

		UserRegistrationResponse response = new UserRegistrationResponse();

		Address address = new Address();
		address.setCity(userRequest.getCity());
		address.setPincode(userRequest.getPincode());
		address.setStreet(userRequest.getStreet());

		Address addAddress = addressDao.save(address);

		User user = new User();
		user.setAddress(addAddress);
		user.setEmailId(userRequest.getEmailId());
		user.setFirstName(userRequest.getFirstName());
		user.setLastName(userRequest.getLastName());
		user.setPhoneNo(userRequest.getPhoneNo());
		user.setPassword(userRequest.getPassword());
		user.setRole(userRequest.getRole());
		user.setWalletAmount(BigDecimal.ZERO);

		// User addUser = userDao.save(user);

		String otp = Helper.generateOTP();
		System.out.println("SENT OTP: " + otp);

		OtpVerification otpVerification = new OtpVerification();
		otpVerification.setEmailId(userRequest.getEmailId());
		otpVerification.setOtp(otp);

		otpVerificationDao.save(otpVerification);

		String toEmail = userRequest.getEmailId();
		String subject = "Art Gallery - Verify Your Email Address for User Registration";
		String message = "User Registration OTP for Art Gallery Website: " + otp + ". Please keep it confidential.";

		System.out.println(subject);
		System.out.println(message);
		
		try {
			this.emailSenderService.sendEmail(toEmail, subject, message);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		

		response.setUser(user);
		response.setResponseMessage("An OTP has been sent to your email. Please verify.");
		response.setResponseCode(ResponseCode.SUCCESS.value());

		System.out.println("response sent!!!");

		return new ResponseEntity<CommanApiResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	

	@PostMapping("verify/register")
	public ResponseEntity<?> verifyAndRegister(@RequestBody UserVerifyRegisterRequest request) {
		System.out.println("recieved request for verify & REGISTER USER");

		UserRegistrationResponse response = new UserRegistrationResponse();

		OtpVerification otpVerification = this.otpVerificationDao.findByEmailId(request.getUser().getEmailId());
		
		if(otpVerification.getOtp().equals(request.getOtp())) {
			User registeredUser = this.userDao.save(request.getUser());
			
			this.otpVerificationDao.delete(otpVerification);
			
			response.setUser(registeredUser);
			response.setResponseMessage("User Registered Successful!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());
			
			return new ResponseEntity<UserRegistrationResponse>(response, HttpStatus.OK);
		}
		
		else {
			this.otpVerificationDao.delete(otpVerification);
			response.setUser(request.getUser());
			response.setResponseMessage("Otp Verification Failed");
			response.setResponseCode(ResponseCode.FAILED.value());
			
			return new ResponseEntity<UserRegistrationResponse>(response, HttpStatus.OK);
		
		}
		
	}

	@PostMapping("login")
	public ResponseEntity<?> loginUser(@RequestBody UserLoginRequest loginRequest) {
		System.out.println("recieved request for LOGIN USER");
		System.out.println(loginRequest);

		User user = new User();
		user = userDao.findByEmailIdAndPasswordAndRole(loginRequest.getEmailId(), loginRequest.getPassword(),
				loginRequest.getRole());

		System.out.println("response sent!!!");
		return ResponseEntity.ok(user);
	}

	@GetMapping("deliveryperson/all")
	public ResponseEntity<?> getAllDeliveryPersons() {
		System.out.println("recieved request for getting ALL Delivery Persons!!!");

		List<User> deliveryPersons = this.userDao.findByRole("Delivery");

		System.out.println("response sent!!!");
		return ResponseEntity.ok(deliveryPersons);
	}

	@GetMapping("supplier/all")
	public ResponseEntity<?> getAllSuppliers() {
		System.out.println("recieved request for getting ALL Delivery Persons!!!");

		List<User> suppliers = this.userDao.findByRole("Supplier");

		System.out.println("response sent!!!");
		return ResponseEntity.ok(suppliers);
	}

	@PostMapping("add/wallet/money")
	public ResponseEntity<CommanApiResponse> addMoneyInWallet(@RequestBody AddWalletMoneyRequestDto request) {
		CommanApiResponse response = new CommanApiResponse();

		if (request == null) {
			response.setResponseMessage("Bad Request, improper request data");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("Bad Request, user id is missing");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getWalletAmount() == 0 || request.getWalletAmount() < 0) {
			response.setResponseMessage("Bad Request, improper data");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User user = userDao.findById(request.getUserId()).get();

		if (user == null) {
			response.setResponseMessage("Bad Request, user not found!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		BigDecimal walletAmount = user.getWalletAmount();
		BigDecimal walletToUpdate = walletAmount.add(BigDecimal.valueOf(request.getWalletAmount()));

		user.setWalletAmount(walletToUpdate);

		User udpatedUser = userDao.save(user);

		if (udpatedUser != null) {
			response.setResponseMessage("Money added in wallet successfully!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.OK);
		} else {
			response.setResponseMessage("Failed to add the money in wallet!!!");
			response.setResponseCode(ResponseCode.SUCCESS.value());

			return new ResponseEntity<CommanApiResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("wallet/fetch")
	public ResponseEntity<String> getCustomerWallet(@RequestParam("userId") int userId) {

		User user = userDao.findById(userId).get();

		String walletAmount = "0.0";

		if (user.getWalletAmount().compareTo(BigDecimal.ZERO) == 0) {
			return new ResponseEntity<>(walletAmount, HttpStatus.OK);
		}

		else {
			return new ResponseEntity<>(String.valueOf(user.getWalletAmount()), HttpStatus.OK);
		}

	}
}
