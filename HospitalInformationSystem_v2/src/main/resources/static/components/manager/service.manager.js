angular.module('hospitalApp.services').factory('managerService', function($http) {
	var service = {
			getUser : getUser,
			getPersonByToken : getPersonByToken,
			getRoleByToken : getRoleByToken,
			updateUser : updateUser ,
			changePassword : changePassword,
			getAllOperationsPage : getAllOperationsPage,
			getNewOperationsPage : getNewOperationsPage,
			getAllExaminationsPage : getAllExaminationsPage,
			getNewExaminationsPage : getNewExaminationsPage,
			getAllPaymentsPage : getAllPaymentsPage,
			getAllDoctors : getAllDoctors,
			saveNewPayment : saveNewPayment
	}
	return service;
	
	function getUser(id) {
		return $http.get('managers/' + id);
	}
	
	function updateUser(user) {
		return $http.put('managers', user);
	}
	
	function changePassword(passwords) {
		return $http.put('/persons/password', passwords);
	}
	
	function getPersonByToken() {
		return $http.get('persons/personByToken')
	}
	
	function getRoleByToken() {
		return $http.get('persons/roleByToken')
	}
	
	function getAllOperationsPage(page) {
		return $http.get('/operations/all?page=' + page)
	}
	
	function getNewOperationsPage(page) {
		return $http.get('/operations/newOperations?page=' + page)
	}
	
	function getAllExaminationsPage(page) {
		return $http.get('/examinations/all?page=' + page)
	}
	
	function getNewExaminationsPage(page) {
		return $http.get('/examinations/newExaminations?page=' + page)
	}
	
	function getAllPaymentsPage(page) {
		return $http.get('/payments/all?page=' + page)
	}
	
	function getAllDoctors() {
		return $http.get('/medicalstaff/all')
	}
	
	function saveNewPayment(newPayment) {
		return $http.post('/payments', newPayment)
	}
	
	


})
