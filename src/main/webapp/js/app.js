/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


$(".modal").draggable({
    handle: ".modal-header"
});

//Clear modal content for reuse the wrapper by other functions
$('body').on('hidden.bs.modal', '.modal', function () {
    $(this).removeData('bs.modal');
});

/*Map layer configurations*/
var map, position1Clicked = false, position2Clicked = false, domain;

initialLoad();

function initialLoad() {
    if (document.getElementById('map') == null) {
        setTimeout(initialLoad, 500); // give everything some time to render
    } else {
        initializeMap();
        // getTileServers();
        // loadWms();
        processAfterInitializationMap();
        //Access gps and make zoom to server location as map center
        //navigator.geolocation.getCurrentPosition(success, error);
        $("#loading").hide();
    }
}


//function success(position) {
//    var browserLatitude = position.coords.latitude;
//    var browserLongitude = position.coords.longitude;
//    map.setView([browserLatitude, browserLongitude]);
//    map.setZoom(14);
//    $.UIkit.notify({
//        message: "Map view set to browser's location",
//        status: 'info',
//        timeout: ApplicationOptions.constance.NOTIFY_INFO_TIMEOUT,
//        pos: 'top-center'
//    });
//}
//
//function error() {
//    $.UIkit.notify({
//        message: "Unable to find browser location!",
//        status: 'warning',
//        timeout: ApplicationOptions.constance.NOTIFY_WARNING_TIMEOUT,
//        pos: 'top-center'
//    });
//}


function initializeMap() {
    domain = window.location.hostname;
    if (typeof(map) !== 'undefined') {
        map.remove();
    }

    $("#position_1").click(function() {
        position1Clicked = true;
        position2Clicked = false;
    });

    $("#position_2").click(function() {
        position2Clicked = true;
        position1Clicked = false;
    });


    if (document.getElementById('map') == null) {
        console.log("no map");
    } else {
    }

    map = L.map("map", {
        zoom: 16,
        center:[34.0363282,-84.460727],
        layers: [defaultOSM, defaultTFL],
        zoomControl: false,
        attributionControl: false,
        maxZoom: 20,
        maxNativeZoom: 18
    });

    $("#running_total_btn").click(function() {
        var requestBody = "{" +
            "\"id\":\""+$("#device_id").val()+"\"," +
            "\"analyticsTableName\":\"IOTDOTS_RUNNING_TOTAL\"" +
            "}";

        (function poll(){
            setInterval(function(){
                $.ajax({
                    url: 'https://'+domain+':9445/org.iot.dots.service-1.0-SNAPSHOT/rest/service/running_total',
                    type: 'POST',
                    datatype:'json',
                    contentType: "application/json",
                    data: requestBody, // or $('#myform').serializeArray()
                    success: function (data, status, jqXHR) {
                        console.log(data);
                        $('#running_total').val(data);
                    },
                    error: function (jqXHR, status) {
                        // error handler
                        console.log(jqXHR);
                        console.error('fail' + status.code);
                    }
                });
            }, 2000);
        })();
    });


    $("#calculate_dis_btn").click(function() {
        console.log("clicked calculate button");
        var point1 = $("#position_1").val().split(',');
        var point2 = $("#position_2").val().split(',');
        var requestBody = "{" +
            "\"latitude1\":"+point1[0]+"," +
            "\"latitude2\":"+point2[0]+"," +
            "\"longitude1\":"+point1[1]+"," +
            "\"longitude2\":"+point2[1]+"," +
            "\"id\":\""+$("#device_id").val()+"\"," +
            "\"analyticsTableName\":\"IOT_DOTS_POINTDISTANCESTREAM\"" +
            "}";

        $.ajax({
            url: 'https://'+domain+':9445/org.iot.dots.service-1.0-SNAPSHOT/rest/service/distances',
            type: 'POST',
            datatype:'json',
            contentType: "application/json",
            data: requestBody, // or $('#myform').serializeArray()
            success: function (data, status, jqXHR) {
                console.log(data);
                for (var i=0; i<data.length; i++) {
                    var pointList = [];
                    var path = data[i].path;
                    for (var j=0; j<path.length; j++) {
                        // var newLineStringGeoJson = this.createLineStringFeature(
                        //     "distance",
                        //     "normal",
                        //     [path[j].latitude, path[j].longitude]);
                        // this.pathGeoJsons.push(newLineStringGeoJson);
                        
                        // console.log(path[j].latitude + " - " + path[j].longitude);

                        // var marker = L.marker([path[j].latitude,path[j].longitude]).addTo(map);

                        var pointA = new L.LatLng(path[j].latitude, path[j].longitude);
                        pointList.push(pointA);
                    }
                    console.log(pointList);
                    if (i==0) {
                        var firstpolyline = new L.polyline(pointList, {
                            color: 'red',
                            weight: 5,
                            opacity: 0.5,
                            smoothFactor: 1
                        });
                        // firstpolyline.bindPopup(data[i].total_distance);
                        var popupTemplate = $('#markerPopup');
                        popupTemplate.find('#distance').html(data[i].total_distance);
                        firstpolyline.bindPopup(popupTemplate.html());
                        firstpolyline.addTo(map);
                    } else {
                        var firstpolyline = new L.polyline(pointList, {
                            color: 'blue',
                            weight: 5,
                            opacity: 0.5,
                            smoothFactor: 1
                        });
                        var popupTemplate = $('#markerPopup');
                        popupTemplate.find('#distance').html(data[i].total_distance);
                        firstpolyline.bindPopup(popupTemplate.html());
                        console.log(data[i].total_distance);
                        firstpolyline.addTo(map);
                    }
                }
            },
            error: function (jqXHR, status) {
                // error handler
                console.log(jqXHR);
                console.error('fail' + status.code);
            }
        });
    });

    map.on('click', function (e) {
        $.UIkit.offcanvas.hide();//[force = false] no animation
        if (position1Clicked == true) {
            var coord = e.latlng.toString().split(',');
            var lat = coord[0].split('(');
            var lng = coord[1].split(')');
            $("#position_1").val(lat[1] +"," + lng[0]);
            position1Clicked = false;
        } else if (position2Clicked == true) {
            var coord = e.latlng.toString().split(',');
            var lat = coord[0].split('(');
            var lng = coord[1].split(')');
            $("#position_2").val(lat[1] +"," + lng[0]);
            position2Clicked = false;
        }
    });

    map.on('zoomend', function () {
        if (map.getZoom() < 14) {
            // remove busStops
            // var layer;
            // for (var key in currentSpatialObjects) {
            //     if (currentSpatialObjects.hasOwnProperty(key)) {
            //         object = currentSpatialObjects[key];
            //         if (object.type == "STOP")
            //             map.removeLayer(object.geoJson);
            //     }
            // }
            // console.log("removed busStops from map");
        } else {

            // var layer;
            // for (var key in currentSpatialObjects) {
            //     if (currentSpatialObjects.hasOwnProperty(key)) {
            //         object = currentSpatialObjects[key];
            //         if (object.type == "STOP")
            //             map.addLayer(object.geoJson);
            //     }
            // }
            // console.log("added busStops to map");
        }

    });


}

function createLineStringFeature (state, information, coordinates) {
    return {
        "type": "Feature",
        "properties": {
            "state": state,
            "information": information
        },
        "geometry": {
            "type": "LineString",
            "coordinates": [coordinates]
        }
    };
}

/* Attribution control */
function updateAttribution(e) {
    $.each(map._layers, function (index, layer) {
        if (layer.getAttribution) {
            $("#attribution").html((layer.getAttribution()));
        }
    });
}

var attributionControl;
var groupedOverlays;
var layerControl;

function processAfterInitializationMap(){
    attributionControl = L.control({
        position: "bottomright"
    });
    attributionControl.onAdd = function (map) {
        var div = L.DomUtil.create("div", "leaflet-control-attribution");
        div.innerHTML = "<a href='#' onclick='$(\"#attributionModal\").modal(\"show\"); return false;'>Attribution</a>";
        return div;
    };
    //map.addControl(attributionControl);

    //L.control.fullscreen({
    //    position: 'bottomright'
    //}).addTo(map);
    L.control.zoom({
        position: "bottomright"
    }).addTo(map);

    groupedOverlays = {
        "Web Map Service layers": {}
    };

    layerControl = L.control.groupedLayers(baseLayers, groupedOverlays, {
        collapsed: true
    }).addTo(map);

    //L.control.layers(baseLayers).addTo(map);
    //map.addLayer(defaultTFL);
}

/* Highlight search box text on click */
$("#searchbox").click(function () {
    $(this).select();
});
/* TypeAhead search functionality */

var substringMatcher = function () {
    return function findMatches(q, cb) {
        var matches, substrRegex;
        matches = [];
        substrRegex = new RegExp(q, 'i');
        $.each(currentSpatialObjects, function (i, str) {
            if (substrRegex.test(i)) {
                matches.push({value: i});
            }
        });

        cb(matches);
    };
};

var chart;
function createChart() {
    chart = c3.generate({
        bindto: '#chart_div',
        data: {
            columns: [
                ['speed']
            ]
        },
        subchart: {
            show: true
        },
        axis: {
            y: {
                label: {
                    text: 'Speed',
                    position: 'outer-middle'
                }
            }
        },
        legend: {
            show: false
        }
    });
}

var predictionChart;
function createPredictionChart() {
    predictionChart = c3.generate({
        bindto: '#prediction_chart_div',
        data: {
            x: 'x',
            columns: [
                ['traffic']
            ]
        },
        subchart: {
            show: true
        },
        axis: {
            y: {
                label: {
                    text: 'Traffic',
                    position: 'outer-middle'
                }
            },
            x: {
                label: {
                    text: 'UTC hour for today',
                    position: 'outer-middle'
                }
            }

        },
        legend: {
            show: false
        }
    });
}

$('#searchbox').typeahead({
        hint: true,
        highlight: true,
        minLength: 1
    },
    {
        name: 'speed',
        displayKey: 'value',
        source: substringMatcher()
    }).on('typeahead:selected', function ($e, datum) {
        objectId = datum['value'];
        focusOnSpatialObject(objectId)
    });

var toggled = false;
function focusOnSpatialObject(objectId) {
    
    // console.log("Selecting" + objectId);
    // var spatialObject = currentSpatialObjects[objectId];// (local)
    // if (!spatialObject) {
    //     $.UIkit.notify({
    //         message: "Spatial Object <span style='color:red'>" + objectId + "</span> not in the Map!!",
    //         status: 'warning',
    //         timeout: ApplicationOptions.constance.NOTIFY_WARNING_TIMEOUT,
    //         pos: 'top-center'
    //     });
    //     return false;
    // }
    // clearFocus(); // Clear current focus if any
    // selectedSpatialObject = objectId; // (global) Why not use 'var' other than implicit declaration http://stackoverflow.com/questions/1470488/what-is-the-function-of-the-var-keyword-and-when-to-use-it-or-omit-it#answer-1471738
    //
    // console.log("Selected " + objectId + " type " + spatialObject.type);
    // if (spatialObject.type == "area") {
    //     spatialObject.focusOn(map);
    //     return true;
    // }
    //
    // map.setView(spatialObject.marker.getLatLng(), 15, {animate: true}); // TODO: check the map._layersMaxZoom and set the zoom level accordingly
    //
    // $('#objectInfo').find('#objectInfoId').html(selectedSpatialObject);
    // spatialObject.marker.openPopup();
    // if (!toggled) {
    //     $('#objectInfo').animate({width: 'toggle'}, 100);
    //     toggled = true;
    // }
    // getAlertsHistory(objectId);
    // spatialObject.drawPath();
    // setTimeout(function () {
    //     createChart();
    //     chart.load({columns: [spatialObject.speedHistory.getArray()]});
    // }, 100);
}


// Unfocused on current searched spatial object
function clearFocus() {
    if (selectedSpatialObject) {
        spatialObject = currentSpatialObjects[selectedSpatialObject];
        spatialObject.removeFromMap();
        selectedSpatialObject = null;
    }
}