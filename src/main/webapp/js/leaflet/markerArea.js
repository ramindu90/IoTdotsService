/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

function GeoAreaObject(json) {
    this.id = json.id;
    this.type = "area";

    var myStyle = {
        "color": "#000001",
        "weight": 5,
        "opacity": 0,
        "fillOpacity": 0.75
    };

    switch (json.properties.state) {
        case "Moderate":
            myStyle["color"] = "#ffb13b";
            break;
        case "Severe":
            myStyle["color"] = "#ff3f3f";
            break;
        case "Minimal":
            return null;
    }

    this.geoJson = L.geoJson(json, {style: myStyle});
    this.marker = this.geoJson.getLayers()[0];
    this.marker.options.title = this.id;
    this.popupTemplate = $('#areaPopup');
    this.popupTemplate.find('#objectId').html(this.id);
    this.popupTemplate.find('#severity').html(json.properties.state);
    this.popupTemplate.find('#information').html(json.properties.information);
    this.marker.bindPopup(this.popupTemplate.html());
    return this;
}

GeoAreaObject.prototype.addTo = function (map) {
    this.geoJson.addTo(map);
};

GeoAreaObject.prototype.focusOn = function (map) {
    map.fitBounds(this.geoJson);
};

GeoAreaObject.prototype.removeFocusFromMap = function () {
    map.removeLayer(this.geoJson);
};

GeoAreaObject.prototype.update = function (geoJSON) {

    this.information = geoJSON.properties.information;
    this.type = geoJSON.properties.type;

    // Update the spatial object leaflet marker
    this.marker.setLatLng([this.latitude, this.longitude]);
    this.marker.setIconAngle(this.heading);
    this.marker.setIcon(this.stateIcon());

    console.log("update called");
    // TODO: use general popup DOM
    this.popupTemplate.find('#objectId').html(this.id);
    this.popupTemplate.find('#information').html(this.information);

    this.marker.setPopupContent(this.popupTemplate.html())
};