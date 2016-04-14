
var mailCore = new (function () {

    var THIS = this;
    var functionsAfterLogin = [];
    var latestView = 'MAIN';

    this.mailPageLength = 20;

    this.loginAndRetry = function (fnToExecute) {
        functionsAfterLogin.push(fnToExecute);
        THIS.showLogin();
    };

    this.executePendingsAfterLogin = function() {
        while(functionsAfterLogin.length) functionsAfterLogin.shift()();
    };

    this.showLoading = function(skipAsLatestView) {
        $('.mail-load-waiting').show();
        $('.mail-login').css('display', 'none');
        $('.mail-main').hide();
        $('.mail-error').hide();
        if(!skipAsLatestView) latestView = 'LOADING';
    };

    this.showLogin = function() {
        $(".mail-load-waiting").hide();
        $(".mail-login").css('display', 'table');
        $(".mail-main").hide();
        $('.mail-error').hide();
    };

    this.showMain = function() {
        $(".mail-load-waiting").hide();
        $(".mail-login").css('display', 'none');
        $(".mail-main").show();
        $('.mail-error').hide();
        latestView = 'MAIN';
    };

    this.showError = function() {
        $(".mail-load-waiting").hide();
        $(".mail-login").css('display', 'none');
        $(".mail-main").hide();
        $('.mail-error').show();
    };

    this.showLatest = function() {
        if(latestView == 'MAIN')
            THIS.showMain();
        else if(latestView == 'LOADING')
            THIS.showLoading();
    };

    this.createEmailTreeController = function(domElementId, scope, mailService) {

        THIS.showLoading();

        mailService.getStartingFolder('', 0, function (result) {// TODO unhardcode the 0
            rearrangeFolderData(scope, result.data);
            var screenData = scope.folder;

            var ulNode = document.createElement("ul");
            ulNode.setAttribute('class', 'mail-tree');
            if(scope.root.name != "") {
                appendTreeNode(ulNode, scope.root, screenData.fullName, onClick);
            } else {
                var folder = null;
                var domElement = null;
                scope.root.subFolders.forEach(function(item) {
                    var node = appendTreeNode(ulNode, item, 'INBOX', onClick);// TODO Sure it will always going to be INBOX?
                    if(item.fullName == 'INBOX') {
                        folder = item;
                        domElement = node;
                    }
                });
                if(folder) {
                    scope.folder = folder;
                    getMessages(folder.fullName, domElement, 0); // TODO unhardcode the 0
                } else {
                    // TODO error
                }

            }
            document.getElementById(domElementId).appendChild(ulNode);

            THIS.showMain();
        });

        function onClick(event) {
            event.stopPropagation();
            var domElement = this;
            var ulNode = getNthElement(this, 1);
            var fullName = this.getAttribute('data-full-name');
            var expanded = this.getAttribute('data-is-expanded') == 'true';
            var selected = this.getAttribute('class').indexOf('mail-tree-node-selected') != -1;
            var loaded = this.getAttribute('data-is-loaded') == 'true';

            if(!selected) {
                var selectedElement = $('.mail-tree-node-selected')[0];
                selectedElement.setAttribute('class', 'mail-tree-node');
                this.setAttribute('class', 'mail-tree-node mail-tree-node-selected');
                getMessages(fullName, domElement, 0); // TODO unhardcode the 0
            } else if(expanded) {
                $(ulNode).hide(200);
                this.setAttribute('data-is-expanded', 'false');
            } else if(loaded) {
                $(ulNode).show(200);
                this.setAttribute('data-is-expanded', 'true');
            } else {
                THIS.showMain();
                addLoadingGif(domElement);
                mailService.getSubfolders(fullName, function (result) {
                    result.data.forEach(function(item) { appendTreeNode(ulNode, item, '', onClick) });
                    domElement.setAttribute('data-is-expanded', 'true');
                    domElement.setAttribute('data-is-loaded', 'true');
                    removeLoadingGif(domElement);
                    $(ulNode).show(200);
                });
                this.setAttribute('data-is-expanded', 'true');
                this.setAttribute('data-is-loaded', 'true');
            }
        }

        function getMessages(fullFolderName, domElement, pageIndex) {
            THIS.showMain();
            addLoadingGif(domElement);
            mailService.getFolder(fullFolderName, pageIndex, function (result) {
                scope.folder = result.data;
                removeLoadingGif(domElement);
            });
        }

        function appendTreeNode(ulNode, root, selected, clickListener) {

            return appendTreeNode(ulNode, root);

            function appendTreeNode(ulNode, folder) {
                var node = document.createElement("li");
                var nodeText = document.createElement("div");
                var nodeList = document.createElement("ul");

                nodeList.setAttribute('class', 'mail-tree');
                nodeText.appendChild(document.createTextNode(folder.name));
                node.setAttribute('class', 'mail-tree-node' + (folder.fullName == selected? ' mail-tree-node-selected': ''));
                node.setAttribute('data-full-name', folder.fullName);
                node.appendChild(nodeText);
                node.appendChild(nodeList);
                node.addEventListener('click', clickListener);
                node.addEventListener('dblclick', function(e){ e.preventDefault(); });
                ulNode.appendChild(node);

                if(folder.subFolders) {
                    node.setAttribute('data-is-loaded', 'true');
                    node.setAttribute('data-is-expanded', 'true');
                    folder.subFolders.forEach(function (item) { appendTreeNode(nodeList, item); });
                } else {
                    node.setAttribute('data-is-loaded', 'false');
                    node.setAttribute('data-is-expanded', 'false');
                    $(nodeList).hide();
                }

                return node;
            }
        }

    };

    function rearrangeFolderData(scope, folderData) {
        var parent = folderData.parent;
        var current = {parent: folderData.parent, name: folderData.name, fullName: folderData.fullName,
            subFolders: folderData.subFolders};
        while(parent) {
            parent.subFolders.push(current);
            parent.subFolders.sort(function (folder1, folder2) {
                return folder1.fullName < folder2.fullName ? -1:(folder1.fullName > folder2.fullName? 1: 0);
            });
            delete current.parent;
            current = parent;
            parent = current.parent;
        }
        scope.root = current;
        scope.folder = folderData;
        delete folderData.parent;
        delete folderData.subFolders;
    }

    function addLoadingGif(domElement) {
        var imgLoading = document.createElement('img');
        imgLoading.setAttribute('src', 'image/loading-subfolders.gif');
        getNthElement(domElement, 0).appendChild(imgLoading);
    }

    function removeLoadingGif(domElement) {
        var textPart = getNthElement(domElement, 0);
        textPart.removeChild(textPart.childNodes.item(textPart.childNodes.length - 1));
    }

    function getNthElement(domElement, childNumber) {
        var elementNumber = 0;
        var x;
        for(x = 0; x < domElement.childNodes.length; x++) {
            var child = domElement.childNodes.item(x);
            if(child.nodeType == 1) {
                if(childNumber == elementNumber) return child;
                elementNumber++;
            }
        }
        return null;
    }

})();


angular.module('mail', ['ngRoute'])

    .config(function($routeProvider) {
        $routeProvider
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

        service.getStartingFolder = function(fullFolderName, startingPageIndex, callback) {
callback({data: {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:0, subFolders:[{unreadMessages:84, newMessages:255, pageMessages:null, totalMessages:584, subFolders:null, fullName:"Trash", name:"Trash", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:15, subFolders:null, fullName:"Archivo", name:"Archivo", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:87, subFolders:null, fullName:"Sent", name:"Sent", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:0, subFolders:null, fullName:"Drafts", name:"Drafts", parent:null}, {unreadMessages:117, newMessages:0, pageMessages:null, totalMessages:297, subFolders:null, fullName:"INBOX", name:"INBOX", parent:null}], fullName:"", name:"", parent:null}});
// TODO unhardcode
//             $http({url: "api/mail/start", method: "GET", params: {fullFolderName: fullFolderName,
//                     startingPageIndex: startingPageIndex, pageLength: mailCore.mailPageLength}}).then(
//                 callback,
//                 onError(function() {service.getStartingFolder(fullFolderName, startingPageIndex, callback);})
//             );
        };

        service.getSubfolders = function(fullFolderName, callback) {
callback({data: [{unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:0, subFolders:null, fullName:"INBOX.Clui", name:"Clui", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:0, subFolders:null, fullName:"INBOX.testingfolder", name:"testingfolder", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:1336, subFolders:null, fullName:"INBOX.mdh", name:"mdh", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:14, subFolders:null, fullName:"INBOX.scrum", name:"scrum", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:0, subFolders:null, fullName:"INBOX.seek_just_seek", name:"seek_just_seek", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:8, subFolders:null, fullName:"INBOX.socialCoder", name:"socialCoder", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:0, subFolders:null, fullName:"INBOX.subscriptions", name:"subscriptions", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:4, subFolders:null, fullName:"INBOX.Queensland JVM", name:"Queensland JVM", parent:null}, {unreadMessages:0, newMessages:0, pageMessages:null, totalMessages:23, subFolders:null, fullName:"INBOX.Atlassian", name:"Atlassian", parent:null}]});
// TODO unhardcode
//             $http({url: "api/mail/subFolders", method: "GET", params: {fullFolderName: fullFolderName}}).then(
//                 callback, onError(function() {service.getSubfolders(fullFolderName, callback)})
//             );
        };

        service.getFolder = function(fullFolderName, startingPageIndex, callback) {
callback({data: {unreadMessages:117, newMessages:0, pageMessages:[{sentDate:{time:1384386883000, year:113, month:10, date:14, hours:9, minutes:54, seconds:43, day:4, timezoneOffset:-600}, subject:"Activa tu suscripci\xF3n a: Emprendi\xE9ndole", from:["FeedBurner Email Subscriptions <noreply+feedproxy@google.com>"]}, {sentDate:{time:1395012026000, year:114, month:2, date:17, hours:9, minutes:20, seconds:26, day:1, timezoneOffset:-600}, subject:"Welcome to Entrepreneur Inspiration Station", from:["inspiration@entrepreneur.com"]}, {sentDate:{time:1404975464000, year:114, month:6, date:10, hours:16, minutes:57, seconds:44, day:4, timezoneOffset:-600}, subject:"pic", from:["Rafael Villar Villar <morgano5@gmail.com>"]}, {sentDate:{time:1411073033000, year:114, month:8, date:19, hours:6, minutes:43, seconds:53, day:5, timezoneOffset:-600}, subject:"Message accepted: RE: Reference request about Rype", from:["Chris Palmer <hit-reply@linkedin.com>"]}, {sentDate:{time:1411106461000, year:114, month:8, date:19, hours:16, minutes:1, seconds:1, day:5, timezoneOffset:-600}, subject:"[Bitbucket] Confirm your email address", from:["Bitbucket <noreply@bitbucket.org>"]}, {sentDate:{time:1411119980000, year:114, month:8, date:19, hours:19, minutes:46, seconds:20, day:5, timezoneOffset:-600}, subject:"Your new myGov username", from:["myGov <noreply@my.gov.au>"]}, {sentDate:{time:1411143364000, year:114, month:8, date:20, hours:2, minutes:16, seconds:4, day:6, timezoneOffset:-600}, subject:"RE: saludines", from:["M ar <arkadfel@hotmail.com>"]}, {sentDate:{time:1411184486000, year:114, month:8, date:20, hours:13, minutes:41, seconds:26, day:6, timezoneOffset:-600}, subject:"confirm subscribe to announce@apache.org", from:["announce-help@apache.org"]}, {sentDate:{time:1411184623000, year:114, month:8, date:20, hours:13, minutes:43, seconds:43, day:6, timezoneOffset:-600}, subject:"WELCOME to announce@apache.org", from:["announce-help@apache.org"]}, {sentDate:{time:1411502566000, year:114, month:8, date:24, hours:6, minutes:2, seconds:46, day:3, timezoneOffset:-600}, subject:"MOZART RANNA SOVIERZOSKI PMP\xAE 3000+ Lin congratulated you on your work anniversary!", from:["=?UTF-8?Q?MOZART_RANNA_SOVIERZOSKI_PMP=C2=AE_3000+_Lin_via_LinkedIn?= <notifications-noreply@linkedin.com>"]}, {sentDate:{time:1411690834000, year:114, month:8, date:26, hours:10, minutes:20, seconds:34, day:5, timezoneOffset:-600}, subject:"[JCP] Registration: Confirmation Step", from:["JCP Auto Registrar <admin-registrar@JCP.org>"]}, {sentDate:{time:1411691144000, year:114, month:8, date:26, hours:10, minutes:25, seconds:44, day:5, timezoneOffset:-600}, subject:"[JCP] Registration Info", from:["admin@jcp.org"]}, {sentDate:{time:1411729395000, year:114, month:8, date:26, hours:21, minutes:3, seconds:15, day:5, timezoneOffset:-600}, subject:"Rv:Re:Nuevos emails y telefonos", from:["rvillar <rvillar@prodigy.net.mx>"]}, {sentDate:{time:1412911889000, year:114, month:9, date:10, hours:13, minutes:31, seconds:29, day:5, timezoneOffset:-600}, subject:"Hi! A change has been made to your profile", from:["myaccount@optus.com.au"]}, {sentDate:{time:1413198119000, year:114, month:9, date:13, hours:21, minutes:1, seconds:59, day:1, timezoneOffset:-600}, subject:"IMPORTANT: Activate your registration on the Do Not Call Register", from:["donotreply@donotcall.gov.au"]}, {sentDate:{time:1414375409000, year:114, month:9, date:27, hours:12, minutes:3, seconds:29, day:1, timezoneOffset:-600}, subject:"Re: New Role - QSuper - Senior Systems Developer", from:["Ashish Khurana <ashi.khurana@gmail.com>"]}, {sentDate:{time:1414979267000, year:114, month:10, date:3, hours:11, minutes:47, seconds:47, day:1, timezoneOffset:-600}, subject:"Re: NSWFA WS Update and other topics", from:["Graham Lynn <graham@rype.com.au>"]}, {sentDate:{time:1417136524000, year:114, month:10, date:28, hours:11, minutes:2, seconds:4, day:5, timezoneOffset:-600}, subject:"Warning from announce@apache.org", from:["announce-help@apache.org"]}, {sentDate:{time:1417476672000, year:114, month:11, date:2, hours:9, minutes:31, seconds:12, day:2, timezoneOffset:-600}, subject:"Activate your JetBrains Account", from:["JetBrains Account <account@jetbrains.com>"]}, {sentDate:{time:1417476724000, year:114, month:11, date:2, hours:9, minutes:32, seconds:4, day:2, timezoneOffset:-600}, subject:"Your JetBrains Account has been created successfully", from:["JetBrains Account <account@jetbrains.com>"]}], totalMessages:297, subFolders:null, fullName:"INBOX", name:"INBOX", parent:null}});
// TODO unhardcode
//             $http({url: "api/mail/folder", method: "GET", params: {fullFolderName: fullFolderName,
//                     startingPageIndex: startingPageIndex, pageLength: mailCore.mailPageLength}}).then(
//                 callback,
//                 onError(function() {service.getFolder(fullFolderName, startingPageIndex, callback)})
//             );
        };

        function onError(retryFunction) {
            return function(error) {
                if (error.status == 401) {
                    mailCore.loginAndRetry(retryFunction);
                } else {
                    mailCore.showError();
                }
            }
        }

        return service;
    })

    .controller('CoreController', function($scope, $location, mailService) {
        //$scope.doClick = function() { $scope.$broadcast('folderChanged', {uno: 1, dos: 2}) }

        $scope.logout = function() {
            mailCore.showLoading();
            mailService.logout().then( function() { mailCore.showLogin(); }, function() { mailCore.showError(); });
        };

        $scope.nextEmailPage = function() { goToEmailPage($scope.getPageIndex() + 1); };

        $scope.previousEmailPage = function() { goToEmailPage($scope.getPageIndex() - 1); };

        $scope.goToEmailPage =  function() { goToEmailPage($scope.pageNumber - 1); };

        $scope.isLastPage = function() {
            return $scope.getTotalPages() == $scope.getPageIndex() + 1;
        };

        $scope.getPageIndex = function() {
            if(!$scope.folder) $scope.folder = {};
            if(!$scope.folder.pageIndex) $scope.folder.pageIndex = 0;
            return $scope.folder.pageIndex;
        };

        $scope.getTotalPages = function() {
            return Math.floor($scope.folder.totalMessages / mailCore.mailPageLength)
                + ($scope.folder.totalMessages % mailCore.mailPageLength > 0? 1: 0);
        };

        mailCore.createEmailTreeController('mail-folder-tree', $scope, mailService);

        function goToEmailPage(pageIndex) {
            if(pageIndex < 0) pageIndex = 0;
            if(pageIndex >= $scope.getTotalPages()) pageIndex = $scope.getTotalPages() - 1;
            mailCore.showLoading();
            mailService.getFolder($scope.folder.fullName, pageIndex, function (result) {
                $scope.folder = result.data;
                $scope.folder.pageIndex = pageIndex;
                mailCore.showMain();
            });
        }


    })

    .controller('LoginController', function($scope, mailService) {
        $scope.login = function() {
            mailCore.showLoading(true);
            mailService.login($scope.username, $scope.password).then(
                function() {
                    $scope.username = '';
                    $scope.password = '';
                    $scope.loginMessage = '';
                    mailCore.showLatest();
                    mailCore.executePendingsAfterLogin();
                },
                function(error) {
                    if (error.status == 401) {
                        $scope.loginMessage = 'Wrong Username/password';
                        mailCore.showLogin();
                    } else {
                        mailCore.showError();
                    }
                }
            );
        };

    })

    .controller('MailController', function($scope) {

        //$scope.$on('folderChanged', function(event, folder) { alert("Me llega: " + folder.toSource()) });

        $scope.toName = function(emailField) {
            emailField = emailField.trim();

            var startingMail = emailField.indexOf('<');
            if(startingMail == -1) {
                return emailField;
            }

            var endingMail = emailField.indexOf('>');
            if(startingMail > endingMail || endingMail < emailField.length - 1) {
                return emailField;
            }

            if(startingMail == 0) {
                return emailField.substring(startingMail + 1, endingMail);
            }

            return emailField.substring(0, startingMail).trim();
        };

        $scope.toEmail = function(emailField) {
            emailField = emailField.trim();

            var startingMail = emailField.indexOf('<');
            if(startingMail == -1) {
                return emailField.indexOf('@') != -1? emailField: null;
            }

            var endingMail = emailField.indexOf('>');
            if(startingMail > endingMail || endingMail < emailField.length - 1) {
                return null;
            }

            return emailField.indexOf('@') != -1? emailField.substring(startingMail + 1, endingMail).trim(): null;
        };

        $scope.formatDate = function(dateObj) {
            var date = new Date(dateObj.time);
            var year = date.getFullYear();
            var month = date.getMonth() + 1; if (month < 10) month = '0' + month;
            var day = date.getDay(); if (day < 10) day = '0' + day;
            var hour = date.getHours(); if (hour < 10) hour = '0' + hour;
            var minute = date.getMinutes(); if (minute < 10) minute = '0' + minute;
            return year + '-' + month + '-' + day + ' ' + hour + ':' + minute;
        };

    });
