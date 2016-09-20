/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The common controllers
 */
angular.module('niord.common')

    /**
     * Language Controller
     */
    .controller('LangCtrl', ['$scope', '$window', 'LangService',
        function ($scope, $window, LangService) {
            'use strict';

            $scope.changeLanguage = function (lang) {
                LangService.changeLanguage(lang);
                $window.location.reload();
            }

        }])

    /**
     * Domain Controller
     */
    .controller('DomainCtrl', ['$scope', '$window', '$location', '$timeout', 'DomainService',
        function ($scope, $window, $location, $timeout, DomainService) {
            'use strict';

            $scope.changeDomain = function (domain) {
                // Reset all parameters
                $location.url($location.path());

                $timeout(function () {
                    // Change domain
                    DomainService.changeDomain(domain);
                    $window.location.reload();
                });
            }

        }])


    /**
     * Controller handling cookies and disclaimer dialogs
     */
    .controller('FooterCtrl', ['$scope', '$uibModal',
        function ($scope, $uibModal) {
            'use strict';

            $scope.cookiesDlg = function () {
                $uibModal.open({
                    templateUrl: '/app/common/cookies.html',
                    size: 'lg'
                });
            };

            $scope.disclaimerDlg = function () {
                $uibModal.open({
                    templateUrl: '/app/common/disclaimer.html',
                    size: 'lg'
                });
            }
        }]);
