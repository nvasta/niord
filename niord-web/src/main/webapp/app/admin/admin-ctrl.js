/**
 * The admin controllers.
 */
angular.module('niord.admin')

    /**
     * Common Admin Controller
     * Will periodically reload batch status to display the number of running batch jobs in the admin page menu
     */
    .controller('CommonAdminCtrl', ['$scope', '$interval', 'AdminBatchService',
        function ($scope, $interval, AdminBatchService) {
            'use strict';

            $scope.batchStatus = {
                runningExecutions: 0,
                types: []
            };

            // Loads the batch status
            $scope.loadBatchStatus = function () {
                AdminBatchService.getBatchStatus()
                    .success(function (status) {
                        $scope.batchStatus = status;
                    });
            };

            // Reload status every 10 seconds
            if ($scope.hasRole('admin')) {
                // Refresh batch status every 10 seconds
                var interval = $interval($scope.loadBatchStatus, 10 * 1000);

                // Terminate the timer
                $scope.$on("$destroy", function() {
                    if (interval) {
                        $interval.cancel(interval);
                    }
                });

                // Initial load
                $scope.loadBatchStatus();
            }

        }])



    /**
     * Charts Admin Controller
     * Controller for the Admin Charts page
     */
    .controller('ChartsAdminCtrl', ['$scope', 'growl', 'AdminChartService', 'DialogService',
        function ($scope, growl, AdminChartService, DialogService) {
            'use strict';

            $scope.allCharts = [];
            $scope.chart = undefined; // The chart being edited
            $scope.chartFeatureCollection = { type: 'FeatureCollection', features: [] };
            $scope.editMode = 'add';

            // Pagination
            $scope.charts = [];
            $scope.pageSize = 10;
            $scope.currentPage = 1;
            $scope.chartNo = 0;
            $scope.search = '';

            /** Loads the charts from the back-end */
            $scope.loadCharts = function() {
                $scope.chart = undefined;
                AdminChartService
                    .getCharts()
                    .success(function (charts) {
                        $scope.allCharts = charts;
                        $scope.pageChanged();
                    });
            };


            /** Returns if the string matches the given chart property */
            function match(chartProperty, str) {
                var txt = (chartProperty) ? "" + chartProperty : "";
                return txt.toLowerCase().indexOf(str.toLowerCase()) >= 0;
            }


            /** Called whenever chart pagination changes */
            $scope.pageChanged = function() {
                var search = $scope.search.toLowerCase();
                var filteredCharts = $scope.allCharts.filter(function (chart) {
                    return match(chart.chartNumber, search) ||
                        match(chart.internationalNumber, search) ||
                        match(chart.horizontalDatum, search) ||
                        match(chart.name, search);
                });
                $scope.chartNo = filteredCharts.length;
                $scope.charts = filteredCharts.slice(
                    $scope.pageSize * ($scope.currentPage - 1),
                    Math.min($scope.chartNo, $scope.pageSize * $scope.currentPage));
            };
            $scope.$watch("search", $scope.pageChanged, true);


            /** Adds a new chart **/
            $scope.addChart = function () {
                $scope.editMode = 'add';
                $scope.chart = {
                    chartNumber: undefined,
                    internationalNumber: undefined,
                    horizontalDatum: 'WGS84'
                };
                $scope.chartFeatureCollection.features.length = 0;
            };


            /** Edits a chart **/
            $scope.editChart = function (chart) {
                $scope.editMode = 'edit';
                $scope.chart = angular.copy(chart);
                $scope.chartFeatureCollection.features.length = 0;
                if ($scope.chart.geometry) {
                    var feature = {type: 'Feature', geometry: $scope.chart.geometry, properties: {}};
                    $scope.chartFeatureCollection.features.push(feature);
                }
            };


            /** Displays the error message */
            $scope.displayError = function () {
                growl.error("Error saving chart");
            };


            /** Saves the current chart being edited */
            $scope.saveChart = function () {

                // Update the chart geometry
                delete $scope.chart.geometry;
                if ($scope.chartFeatureCollection.features.length > 0 &&
                    $scope.chartFeatureCollection.features[0].geometry) {
                    $scope.chart.geometry = $scope.chartFeatureCollection.features[0].geometry;
                }

                if ($scope.chart && $scope.editMode == 'add') {
                    AdminChartService
                        .createChart($scope.chart)
                        .success($scope.loadCharts)
                        .error($scope.displayError);
                } else if ($scope.chart && $scope.editMode == 'edit') {
                    AdminChartService
                        .updateChart($scope.chart)
                        .success($scope.loadCharts)
                        .error($scope.displayError);
                }
            };


            /** Deletes the given chart */
            $scope.deleteChart = function (chart) {
                DialogService.showConfirmDialog(
                    "Delete Chart?", "Delete chart number '" + chart.chartNumber + "'?")
                    .then(function() {
                        AdminChartService
                            .deleteChart(chart)
                            .success($scope.loadCharts)
                            .error($scope.displayError);
                    });
            }

        }])



    /**
     * Domains Admin Controller
     * Controller for the Admin domains page
     */
    .controller('DomainAdminCtrl', ['$scope', 'growl', 'AuthService', 'AdminDomainService', 'DialogService',
        function ($scope, growl, AuthService, AdminDomainService, DialogService) {
            'use strict';

            $scope.allDomains = [];
            $scope.domains = [];
            $scope.domain = undefined; // The domain being edited
            $scope.editMode = 'add';
            $scope.search = '';


            /** Computes the Keycloak URL */
            $scope.getKeycloakUrl = function() {
                // Template http://localhost:8080/auth/admin/master/console/#/realms/niord/clients
                var url = AuthService.keycloak.authServerUrl;
                if (url.charAt(url.length - 1) != '/') {
                    url += '/';
                }
                return url + 'admin/master/console/#/realms/niord/clients';
            };
            $scope.keycloakUrl = $scope.getKeycloakUrl();


            /** Loads the domains from the back-end */
            $scope.loadDomains = function() {
                $scope.domain = undefined;
                AdminDomainService
                    .getDomains()
                    .success(function (domains) {
                        $scope.allDomains = domains;
                        $scope.searchUpdated();
                    });
            };


            /** Returns if the string matches the given domain property */
            function match(domainProperty, str) {
                var txt = (domainProperty) ? "" + domainProperty : "";
                return txt.toLowerCase().indexOf(str.toLowerCase()) >= 0;
            }


            /** Called whenever search criteria changes */
            $scope.searchUpdated = function() {
                var search = $scope.search.toLowerCase();
                $scope.domains = $scope.allDomains.filter(function (domain) {
                    return match(domain.clientId, search) ||
                        match(domain.name, search);
                });
            };
            $scope.$watch("search", $scope.searchUpdated, true);


            /** Adds a new domain **/
            $scope.addDomain = function () {
                $scope.editMode = 'add';
                $scope.domain = {
                    clientId: undefined,
                    name: undefined
                };
            };


            /** Copies a domain **/
            $scope.copyDomain = function (domain) {
                $scope.editMode = 'add';
                $scope.domain = angular.copy(domain);
                $scope.domain.clientId = undefined;
            };


            /** Edits a domain **/
            $scope.editDomain = function (domain) {
                $scope.editMode = 'edit';
                $scope.domain = angular.copy(domain);
            };


            /** Displays the error message */
            $scope.displayError = function () {
                growl.error("Error saving domain");
            };


            /** Saves the current domain being edited */
            $scope.saveDomain = function () {

                if ($scope.domain && $scope.editMode == 'add') {
                    AdminDomainService
                        .createDomain($scope.domain)
                        .success($scope.loadDomains)
                        .error($scope.displayError);
                } else if ($scope.domain && $scope.editMode == 'edit') {
                    AdminDomainService
                        .updateDomain($scope.domain)
                        .success($scope.loadDomains)
                        .error($scope.displayError);
                }
            };


            /** Deletes the given domain */
            $scope.deleteDomain = function (domain) {
                DialogService.showConfirmDialog(
                    "Delete domain?", "Delete domain ID '" + domain.clientId + "'?")
                    .then(function() {
                        AdminDomainService
                            .deleteDomain(domain)
                            .success($scope.loadDomains)
                            .error($scope.displayError);
                    });
            };


            /** Creates the domain in Keycloak **/
            $scope.createInKeycloak = function (domain) {
                AdminDomainService
                    .createDomainInKeycloak(domain)
                    .success($scope.loadDomains)
                    .error($scope.displayError);
            }

        }])


    /**
     * Batch Admin Controller
     */
    .controller('BatchAdminCtrl', ['$scope', '$interval', '$stateParams', '$uibModal', 'AdminBatchService',
        function ($scope, $interval, $stateParams, $uibModal, AdminBatchService) {
            'use strict';

            $scope.batchStatus = {
                runningExecutions: 0,
                types: []
            };
            $scope.batchName = $stateParams.batchName;
            $scope.pageSize = 5;
            $scope.currentPage = 1;
            $scope.executions = [];
            $scope.searchResult = {
                total: 0,
                size: 0,
                data: []
            };


            // Build a flat list of executions of all batch instances.
            // This will make it easier to present the result in a table
            $scope.buildBatchExecutionList = function () {
                $scope.executions.length = 0;
                angular.forEach($scope.searchResult.data, function (instance) {
                    angular.forEach(instance.executions, function (execution, index) {
                        if (index == 0) {
                            execution.instance = instance;
                        }
                        $scope.executions.push(execution);
                    });
                });
            };

            // Loads the batch status
            $scope.loadBatchStatus = function () {
                AdminBatchService.getBatchStatus()
                    .success(function (status) {
                        $scope.batchStatus = status;
                        $scope.loadBatchInstances();
                    });
            };


            // Refresh batch status every 3 seconds
            var refreshInterval = $interval($scope.loadBatchStatus, 3 * 1000);

            // Terminate the timer
            $scope.$on("$destroy", function() {
                if (refreshInterval) {
                    $interval.cancel(refreshInterval);
                }
            });


            // Loads "count" more batch instances
            $scope.loadBatchInstances = function () {
                if ($scope.batchName) {
                    AdminBatchService.getBatchInstances($scope.batchName, $scope.currentPage - 1, $scope.pageSize)
                        .success(function (result) {
                            $scope.searchResult = result;
                            $scope.buildBatchExecutionList();
                        });
                }
            };


            // Called when the given batch type is displayed
            $scope.selectBatchType = function (name) {
                $scope.batchName = name;
                $scope.currentPage = 1;
                $scope.searchResult = {
                    total: 0,
                    size: 0,
                    data: []
                };
                $scope.executions.length = 0;
                $scope.loadBatchInstances();
            };

            /**
             * public enum BatchStatus {STARTING, STARTED, STOPPING,
			STOPPED, FAILED, COMPLETED, ABANDONED }
             */

            /** Returns the color to use for a given status **/
            $scope.statusColor = function (execution) {
                var status = execution.batchStatus || 'undef';
                switch (status) {
                    case 'STARTING':
                    case 'STARTED':
                        return 'label-primary';
                    case 'STOPPING':
                    case 'STOPPED':
                        return 'label-warning';
                    case 'FAILED':
                        return 'label-danger';
                    case 'COMPLETED':
                        return 'label-success';
                    case 'ABANDONED':
                        return 'label-default';
                    default:
                        return 'label-default';
                }
            };

            // Stops the given batch job
            $scope.stop = function (execution) {
                AdminBatchService
                    .stopBatchExecution(execution.executionId)
                    .success($scope.loadBatchInstances);
            };

            // Restarts the given batch job
            $scope.restart = function (execution) {
                AdminBatchService
                    .restartBatchExecution(execution.executionId)
                    .success($scope.loadBatchInstances);
            };

            // Abandon the given batch job
            $scope.abandon = function (execution) {
                AdminBatchService
                    .abandonBatchExecution(execution.executionId)
                    .success($scope.loadBatchInstances);
            };

            // Download the instance data file
            $scope.download = function (instance) {
                AdminBatchService
                    .getBatchDownloadTicket()
                    .success(function (ticket) {
                        var link = document.createElement("a");
                        link.download = instance.fileName;
                        link.href = '/rest/batch/instance/'
                            + instance.instanceId
                            + '/download/'
                            + encodeURI(instance.fileName)
                            + '?ticket=' + ticket;
                        link.click();
                    });
            };

            // Open the logs dialo
            $scope.showLogFiles = function (instanceId) {
                $uibModal.open({
                    controller: "BatchLogFileDialogCtrl",
                    templateUrl: "/app/admin/batch-log-file-dialog.html",
                    size: 'l',
                    resolve: {
                        instanceId: function () {
                            return instanceId;
                        }
                    }
                });
            }

        }])

    /**
     * Dialog Controller for the Batch job log file dialog
     */
    .controller('BatchLogFileDialogCtrl', ['$scope', 'AdminBatchService', 'instanceId',
        function ($scope, AdminBatchService, instanceId) {
            'use strict';

            $scope.instanceId = instanceId;
            $scope.fileContent = '';
            $scope.selection = { file: undefined };
            $scope.files = [];

            // Load the log files
            AdminBatchService.getBatchLogFiles($scope.instanceId)
                .success(function (fileNames) {
                    $scope.files = fileNames;
                });

            $scope.reloadLogFile = function () {
                var file = $scope.selection.file;
                if (file && file.length > 0) {
                    AdminBatchService.getBatchLogFileContent($scope.instanceId, file)
                        .success(function (fileContent) {
                            $scope.fileContent = fileContent;
                        })
                        .error(function (err) {
                            $scope.fileContent = err;
                        });
                } else {
                    $scope.fileContent = '';
                }
            };

            $scope.$watch("selection.file", $scope.reloadLogFile, true);
        }]);
