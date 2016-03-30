
angular.module('mail', ['ngRoute'])

    .config(function($routeProvider) {
        $routeProvider
            .when('/', {
                templateUrl: 'partial/loading.html',
                controller: 'MailController'
            })
            .when('/login', {
                templateUrl: 'partial/login.html',
                controller: 'MailController'
            })
            .when('/main', {
                templateUrl: 'partial/main.html',
                controller: 'MailController'
            })
            .otherwise({redirectTo: '/main'});
    })

    .factory('mailService', function($http) {
        var service = {};

        service.login = function(username, password) {
            return $http({ url: "api/login", method: "POST", data: {username: username, password: password} });
        };

        service.logout = function() {
            return $http({ url: "api/login", method: "DELETE" });
        };

        return service;
    })

    .controller('MailController', function($scope, $http, $location, mailService) {

        loadStart();

        function loadStart() {
            $http({url: "api/mail/start", method: "GET", params: {"pageLength": 20}}).then(
                function (result) {
                    $scope.startData = result.data;
                    $location.path('/main');
                },
                function (error) {
                    if (error.status == 401) {
                        showLogin();
                    } else {
                        // TODO create error message
                        console.log("ERROR: status " + error.status);
                    }
                }
            );
        }

        function showLogin() {
            $location.path('/login');
        }

        $scope.login = function() {
            mailService.login($scope.loginData.username, $scope.loginData.password).then(
                function(result) {
                    $scope.loginMessage = '';
                    $location.path('/');
                    loadStart();
                },
                function(error) {
                    $scope.loginMessage = 'Wrong Username/password';
                }
            );
        };

        $scope.logout = function() {
            mailService.logout().then(
                function() {
                    $location.path('/login');
                },
                function(error) {
                    // TODO
                    alert("error: " + error.status);
                }
            );
        };

    });
