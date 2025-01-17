package com.app.controllers;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.app.converters.MedicalStaffScheduleConverter;
import com.app.dto.ExaminationOperationDetailsDTO;
import com.app.dto.MedicalStaffScheduleDTO;
import com.app.dto.ObjectIDDTO;
import com.app.dto.PatientDTO;
import com.app.model.Address;
import com.app.model.Examination;
import com.app.model.MedicalStaff;
import com.app.model.Operation;
import com.app.model.Patient;
import com.app.model.Person;
import com.app.model.Record;
import com.app.model.Role;
import com.app.model.RoleMember;
import com.app.security.AESencryption;
import com.app.security.Base64Utility;
import com.app.security.TokenUtils;
import com.app.service.AddressService;
import com.app.service.ExaminationService;
import com.app.service.MedicalStaffService;
import com.app.service.OperationService;
import com.app.service.PatientService;
import com.app.service.PersonService;
import com.app.service.RecordService;
import com.app.service.RoleMemberService;
import com.app.service.RoleService;


@RestController
@RequestMapping(value = "patients")
public class PatientController {
	
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int DEFAULT_PAGE_NUMBER = 0;
	private SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy"); 
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private MedicalStaffService medicalStaffService;
	
	@Autowired
	private AddressService addressService;
	
	@Autowired
	private RecordService recordService;
	
	@Autowired
	private OperationService operationService;
	
	@Autowired
	private ExaminationService examinationService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private TokenUtils tokenUtils;
	
	@Autowired 
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private RoleMemberService roleMemberService;
	
	@Autowired
	private RoleService roleService;
	
	@Autowired
	private AESencryption aesEncription;
	
	@Autowired
	private Base64Utility base64Utility;
	
	
	/** Function that register new patient on system.
	 * @param dto Data about user from form.
	 * @return
	 */
	@PreAuthorize("hasAuthority('Add_new_patient')")
	@RequestMapping(method = RequestMethod.POST, consumes = "application/json")
	public ResponseEntity<ObjectIDDTO> registerPatient(@RequestBody PatientDTO dto) {
		Patient patient = new Patient();
		if (dto.getCountry() != null || dto.getCity() != null || dto.getZipCode() != null || dto.getStreet() != null || dto.getNumber()!= null){
			Address address = new Address();
			if (dto.getCountry() != null)
				address.setCountry(dto.getCountry());
			if(dto.getCity() != null)
				address.setCity(dto.getCity());
			if(dto.getZipCode() != null)
				address.setZipCode(Integer.parseInt(dto.getZipCode()));
			if(dto.getStreet() != null)
				address.setStreet(dto.getStreet());
			if(dto.getNumber() != null)
				address.setNumber(dto.getNumber());
	
			address = addressService.save(address);
			patient.setAddress(address);
		}
		patient.setName(dto.getName());
		patient.setSurname(dto.getSurname());
		patient.setPersonalID(Long.parseLong(dto.getPersonalID()));
		patient.setGender(dto.getGender());
		if(dto.getEmail() != "")
			patient.setEmail(dto.getEmail());
		
		MedicalStaff doctor = medicalStaffService.findOne(dto.getDoctor());
		patient.setChosenDoctor(doctor);
		
		if(dto.getBirthday() != null){
			Date birthday = null;
			try {
				birthday = formatter.parse(dto.getBirthday());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			patient.setBirthday(birthday);
		}
		patient.setUsername(patient.getName().toLowerCase()+patient.getSurname().toLowerCase());
		patient.setPassword(passwordEncoder.encode("lozinka"));

		
		patient = patientService.save(patient);
		
		
		//Patient role
		Role role = roleService.findByName("ROLE_PATIENT");
		RoleMember rm = new RoleMember();
		rm.setPerson(patient);
		rm.setRole(role);
		roleMemberService.save(rm);
		
		
		Record record = new Record();
		record.setId(base64Utility.encode(aesEncription.encrypt( patient.getPersonalID() + ""))); 
		record.setExaminations(new HashSet<Examination>());
		record.setOperations(new HashSet<Operation>());
		
		record = recordService.save(record);
		
		ObjectIDDTO retVal = new ObjectIDDTO(patient.getId());
		return new ResponseEntity<>(retVal, HttpStatus.CREATED);
	}
	
	
	/**
	 * Function that returns all patients pageable.
	 * @param token
	 * @param page
	 * @return
	 */
	@PreAuthorize("hasAuthority('View_all_patients')")
	@RequestMapping(value = "/all", method = RequestMethod.GET)
	public ResponseEntity<Page<Patient>> getAllPatients(@RequestHeader("X-Auth-Token") String token, @PageableDefault(page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE) Pageable page){
		Page<Patient> patients = patientService.findAllPage(page);
		return new ResponseEntity<>(patients, HttpStatus.OK);
	}
	
	
	/**
	 * Function that returns all patients of logged doctor pageable.
	 * @param token
	 * @param page
	 * @return
	 */
	@PreAuthorize("hasAuthority('View_all_patients')")
	@RequestMapping(value = "/my", method = RequestMethod.GET)
	public ResponseEntity<Page<Patient>> getMyPatients(@RequestHeader("X-Auth-Token") String token, @PageableDefault(page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE) Pageable page){
		
		String username = tokenUtils.getUsernameFromToken(token);
		Person person = personService.findByUsername(username);
		
		Page<Patient> patients = patientService.findByChosenDoctor(person.getId(), page);
		return new ResponseEntity<>(patients, HttpStatus.OK);
	}
	
	
	/** 
	 * Function gets data about one patient.
	 * @param token
	 * @param id
	 * @return
	 */
	@PreAuthorize("hasAuthority('View_patient_profile')")
	@RequestMapping(value= "/{id}", method = RequestMethod.GET)
	public ResponseEntity<PatientDTO> getPatient(@RequestHeader("X-Auth-Token") String token, @PathVariable int id){
		Patient p = patientService.findOne(id);
		
		PatientDTO retVal = new PatientDTO();
		if (p != null){
			
			retVal.setName(p.getName());
			retVal.setSurname(p.getSurname());
			if(p.getBirthday() != null)
				retVal.setBirthday(formatter.format(p.getBirthday()));
			retVal.setPersonalID(p.getPersonalID()+"");
			if(p.getGender()!= null)
				retVal.setGender(p.getGender());
			
			if (p.getAddress() != null) {
				Address a = p.getAddress();
				retVal.setCountry(a.getCountry());
				retVal.setCity(a.getCity());
				retVal.setZipCode(a.getZipCode()+"");
				retVal.setStreet(a.getStreet());
				retVal.setNumber(a.getNumber());
			}
			
			retVal.setUsername(p.getUsername());
			if(p.getEmail() != null)
				retVal.setEmail(p.getEmail());
			
			if (p.getChosenDoctor() != null)
				retVal.setDoctor(p.getChosenDoctor().getId());
			else
				retVal.setDoctor(-1);
			
			return new ResponseEntity<>(retVal, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	
	
	/**
	 * Function that returns logged patient.
	 * @param token
	 * @return
	 */
	@PreAuthorize("hasAuthority('View_patient_profile')")
	@RequestMapping( method = RequestMethod.GET)
	public ResponseEntity<PatientDTO> getLoggedPatient(@RequestHeader("X-Auth-Token") String token){
		String username = tokenUtils.getUsernameFromToken(token);
		Person person = personService.findByUsername(username);
		Patient p = patientService.findOne(person.getId());
		
		PatientDTO retVal = new PatientDTO();
		if (p != null){
			
			retVal.setName(p.getName());
			retVal.setSurname(p.getSurname());
			if(p.getBirthday() != null)
				retVal.setBirthday(formatter.format(p.getBirthday()));
			retVal.setPersonalID(p.getPersonalID() + "");
			if(p.getGender() != null)
				retVal.setGender(p.getGender());
			
			if (p.getAddress() != null) {
				Address a = p.getAddress();
				retVal.setCountry(a.getCountry());
				retVal.setCity(a.getCity());
				retVal.setZipCode(a.getZipCode() + "");
				retVal.setStreet(a.getStreet());
				retVal.setNumber(a.getNumber());
			}
			
			retVal.setUsername(p.getUsername());
			if(p.getEmail() != null)
				retVal.setEmail(p.getEmail());
			
			if (p.getChosenDoctor() != null)
				retVal.setDoctor(p.getChosenDoctor().getId());
			else
				retVal.setDoctor(-1);
			
			return new ResponseEntity<>(retVal, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Function that return schedule for logged patient.
	 * @param token
	 * @return
	 */
	@PreAuthorize("hasAuthority('View_patient_schedule')")
	@RequestMapping(value = "/schedule", method = RequestMethod.GET)
	public ResponseEntity<List<MedicalStaffScheduleDTO>> getMySchedule(@RequestHeader("X-Auth-Token") String token){
		
		String username = tokenUtils.getUsernameFromToken(token);
		Person patient = personService.findByUsername(username);
		
		String personalIdEncoded = base64Utility.encode(aesEncription.encrypt( patient.getPersonalID() + ""));
		List<Examination> examinations = examinationService.findByRecordId(personalIdEncoded);
		List<Operation> operations = operationService.findByRecordId(personalIdEncoded);
		
		List<MedicalStaffScheduleDTO> retVal = MedicalStaffScheduleConverter.toSchedule(operations, examinations);
		return new ResponseEntity<>(retVal, HttpStatus.OK);
	}
	
	
	/**
	 * Function that return details of operation or examination.
	 * @param token
	 * @param type
	 * @param id
	 * @return
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws IOException 
	 */
	@PreAuthorize("hasAnyAuthority('View_operation', 'View_examination')")
	@RequestMapping(value = "/operationExaminationDetails/{type}/{id}", method = RequestMethod.GET)
	public ResponseEntity<ExaminationOperationDetailsDTO> getDetails(@RequestHeader("X-Auth-Token") String token, @PathVariable String type, @PathVariable int id) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IOException {
		ExaminationOperationDetailsDTO retVal = new ExaminationOperationDetailsDTO();

		if (type.equalsIgnoreCase("operacija")) {
			Operation operation = operationService.findById(id);
			if (operation == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			
			String encodedPersonalId = operation.getRecordOperation().getId();
			byte[] personalIdBytes = base64Utility.decode(encodedPersonalId);
			byte[] decriptetBytes = aesEncription.decrypt(personalIdBytes);
			String decripted = new String(decriptetBytes);
			Long personalIdDecoded = Long.parseLong(decripted);
			Person patient = personService.findByPersonalID(personalIdDecoded);
			
			if (patient == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			
			retVal.setDate(operation.getDate().toString());
			retVal.setDoctorId(operation.getHeadDoctor().getId());
			retVal.setDoctor(operation.getHeadDoctor().getName() + " " + operation.getHeadDoctor().getSurname());
			retVal.setName(operation.getName());
			retVal.setType(type);
			retVal.setPatient(patient.getName() + " " + patient.getSurname());
			retVal.setPatientId(patient.getId());
		} else {
			Examination examination = examinationService.findById(id);
			if (examination == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			

			String encodedPersonalId = examination.getRecord().getId();
			byte[] personalIdBytes = base64Utility.decode(encodedPersonalId);
			byte[] decriptetBytes = aesEncription.decrypt(personalIdBytes);
			String decripted = new String(decriptetBytes);
			Long personalIdDecoded = Long.parseLong(decripted);
			Person patient = personService.findByPersonalID(personalIdDecoded);
			
			if (patient == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			
			retVal.setDate(examination.getDate().toString());
			retVal.setDoctorId(examination.getDoctor().getId());
			retVal.setDoctor(examination.getDoctor().getName() + " " + examination.getDoctor().getSurname());
			retVal.setName(examination.getName());
			retVal.setType(type);
			retVal.setPatient(patient.getName() + " " + patient.getSurname());
			retVal.setPatientId(patient.getId());
			
			retVal.setSymptons(examination.getSymptons());
			retVal.setDiagnosis(examination.getDiagnosis());
			retVal.setTherapy(examination.getTherapy());
		}

		return new ResponseEntity<>(retVal, HttpStatus.OK);
	}
	
	
	/**
	 * Function that updates patient profile.
	 * @param token
	 * @param dto
	 * @return
	 */
	@PreAuthorize("hasAuthority('Edit_patient_profile')")
	@RequestMapping(method = RequestMethod.PUT, consumes = "application/json")
	public ResponseEntity<Void> changeProfile(@RequestHeader("X-Auth-Token") String token, @RequestBody PatientDTO dto) {
		String username = tokenUtils.getUsernameFromToken(token);
		Person person = personService.findByUsername(username);
		Patient p = patientService.findOne(person.getId());
		
		if (p == null)
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		
		if (dto.getCountry() != null || dto.getCity() != null || dto.getZipCode() != null || dto.getStreet() != null || dto.getNumber()!= null){
			Address address = p.getAddress();
			if (address == null)
				address = new Address();
			if (dto.getCountry() != null)
				address.setCountry(dto.getCountry());
			if(dto.getCity() != null)
				address.setCity(dto.getCity());
			if(dto.getZipCode() != null)
				address.setZipCode(Integer.parseInt(dto.getZipCode()));
			if(dto.getStreet() != null)
				address.setStreet(dto.getStreet());
			if(dto.getNumber() != null)
				address.setNumber(dto.getNumber());
	
			address = addressService.save(address);
			p.setAddress(address);
		}
		
		if(dto.getEmail() != null){
			if (personService.emailUnique(dto.getEmail()))
				p.setEmail(dto.getEmail());
			else {
				if(personService.findByEmail(dto.getEmail()).getId() == p.getId())
					p.setEmail(dto.getEmail());
				else
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		
		if (dto.getUsername() != null){
			if (personService.usernameUnique(dto.getUsername()))
				p.setUsername(dto.getUsername());
			else {
				if(personService.findByUsername(dto.getUsername()).getId() == p.getId())
					p.setUsername(dto.getUsername());
				else
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		
		patientService.save(p);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	
	/**
	 * Function that returns patients based on serach string.
	 * Doctor can search patients by personal ID, name and surname.
	 * @param token
	 * @param page
	 * @param searchData
	 * @return
	 */
	@PreAuthorize("hasAuthority('Search_patients')")
	@RequestMapping(value= "/search/{searchData}", method = RequestMethod.GET)
	public ResponseEntity<Page<Patient>> serachPatients(@RequestHeader("X-Auth-Token") String token, @PageableDefault(page = DEFAULT_PAGE_NUMBER, size = DEFAULT_PAGE_SIZE) Pageable page,@PathVariable String searchData){
		
		Page<Patient> retVal = patientService.findBySearchData(searchData, page);
		return new ResponseEntity<>(retVal, HttpStatus.OK);
	}
	
	
	/**
	 * Function that checks if username is unique in DB.
	 * @param username
	 * @return
	 */
	@PreAuthorize("hasAnyAuthority('Edit_patient_profile', 'Add_new_patient')")
	@RequestMapping(value= "/username", method = RequestMethod.POST)
	public ResponseEntity<Void> checkUsername(@RequestBody String username){
		if (personService.usernameUnique(username))
			return new ResponseEntity<>(HttpStatus.OK);
		else
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Function that checks if email is unique in DB.
	 * @param email
	 * @return
	 */
	@PreAuthorize("hasAnyAuthority('Edit_patient_profile', 'Add_new_patient')")
	@RequestMapping(value= "/email", method = RequestMethod.POST)
	public ResponseEntity<Void> checkEmail(@RequestBody String email){
		if (personService.emailUnique(email))
			return new ResponseEntity<>(HttpStatus.OK);
		else{
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	/**
	 * Function that checks if personal id is unique in DB.
	 * @param personalID
	 * @return
	 */
	@PreAuthorize("hasAnyAuthority('Edit_patient_profile', 'Add_new_patient')")
	@RequestMapping(value= "/personalID", method = RequestMethod.POST)
	public ResponseEntity<Void> checkPID(@RequestBody String personalID){
		if (personService.personalIDUnique(personalID)){
			return new ResponseEntity<>(HttpStatus.OK);
		}
		else{
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	/**
	 * Function that checks if birthday is valid.
	 * @param birthday
	 * @return
	 */
	@PreAuthorize("hasAnyAuthority('Edit_patient_profile', 'Add_new_patient')")
	@RequestMapping(value= "/birthday", method = RequestMethod.POST)
	public ResponseEntity<Void> checkBirthday(@RequestBody String birthday){
		Date bday = null;
		try {
			bday = formatter.parse(birthday);
		} catch (ParseException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (bday.after(new Date())){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		else {
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}
	
	/**
	 * Function that checks if date is valid.
	 * @param date
	 * @return
	 */
	@PreAuthorize("hasAnyAuthority('Edit_patient_profile', 'Add_new_patient')")
	@RequestMapping(value= "/dateCheck", method = RequestMethod.POST)
	public ResponseEntity<Void> checkDate(@RequestBody String date){
		Date d = null;
		try {
			d = formatter.parse(date);
		} catch (ParseException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		if (d.before(new Date())){
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		else {
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}
}
