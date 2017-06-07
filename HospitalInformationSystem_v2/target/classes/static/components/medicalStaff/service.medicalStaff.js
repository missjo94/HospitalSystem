angular.module('hospitalApp.services').factory('medicalStaffService',
		function($http) {
			var service = {
				getPatients : getPatients,
				getMyPatients : getMyPatients,
				getPatientsBySearchData : getPatientsBySearchData,
				saveOperation : saveOperation,
				saveExamination : saveExamination,
				deleteOperation : deleteOperation,
				deleteExamination : deleteExamination,
				getOperationExaminationDetails : getOperationExaminationDetails,
				getSchedule : getSchedule,
				checkEmail : checkEmail,
				checkBirthday : checkBirthday,
				checkPID : checkPID,
				getDoctors : getDoctors,
				addPatient : addPatient,
			}
			return service;

			function getPatients() {
				return $http.get('patients/all');
			}
			
			function getMyPatients() {
				return $http.get('patients/my');
			}
			
			function getPatientsBySearchData(searchData) {
				return $http.get('patients/search/' + searchData);
			}
			
			function saveOperation(operation) {
				return $http.post('operations/saveOperation', operation);
			}
			
			function saveExamination(examination) {
				return $http.post('examinations/saveExamination', examination);
			}
			
			function deleteOperation(id) {
				return $http.delete('operations/' + id);
			}
			
			function deleteExamination(id) {
				return $http.delete('examinations/' + id);
			}
			
			function getOperationExaminationDetails(type, id) {
				return $http.get('medicalstaff/operationExaminationDetails/' + type + "/" + id);
			}
			
			function getSchedule() {
				return $http.get('medicalstaff/schedule');
			}
			
			function checkEmail(email) {
				return $http.post('patients/email',email);
			}
			
			function checkBirthday(birthday) {
				return $http.post('patients/birthday',birthday);
			}
			
			function checkPID(personalID) {
				return $http.post('patients/personalID',personalID);
			}
			
			function getDoctors() {
				return $http.get('medicalstaff/all');
			}
			
			function addPatient(patient) {
				return $http.post('patients',patient);
			}

		})
