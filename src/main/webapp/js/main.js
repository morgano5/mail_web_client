
angular.module('mail', [])

    .factory('loginService', function($http) {
        return $http({ url: "api/login", method: "POST" });
    })

    .factory('logoutService', function($http) {
        return $http({ url: "api/login", method: "DELETE" });
    })

    .controller('MailController', function($scope, $http) {

        $http({ url: "api/mail/start", method: "GET", params: {"pageLength": 20} }).then(
            function(result) {
                $scope.startData = result.data;
                // TODO refactor this to use views
                $('.mail-load-waiting').hide();
                $('.mail-main').show();
                $('.mail-login').hide();
            }, function(error) {
                if(error.status == 401) {
                    // TODO refactor this to use views
                    $('.mail-load-waiting').hide();
                    $('.mail-main').hide();
                    $('.mail-login').show();
                } else {
                    // TODO create error message
                    console.log("ERROR: status " + error.status);
                }
            }
        );

        $scope.test = "hey there";
    });
