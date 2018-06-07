package com.atm_search.cseh_17.geld_kompass.ModelOSM;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Tags {
        @SerializedName("amenity")
        @Expose
        private String amenity;
        @SerializedName("atm")
        @Expose
        private String atm;
        @SerializedName("name")
        @Expose
        private String name;
        @SerializedName("operator")
        @Expose
        private String operator;
        @SerializedName("wheelchair")
        @Expose
        private String wheelchair;
        @SerializedName("addr:city")
        @Expose
        private String addrCity;
        @SerializedName("addr:housenumber")
        @Expose
        private String addrHousenumber;
        @SerializedName("addr:postcode")
        @Expose
        private String addrPostcode;
        @SerializedName("addr:street")
        @Expose
        private String addrStreet;
        @SerializedName("opening_hours")
        @Expose
        private String openingHours;
        @SerializedName("addr:country")
        @Expose
        private String addrCountry;
        @SerializedName("building")
        @Expose
        private String building;
        @SerializedName("building:use")
        @Expose
        private String buildingUse;

        public String getAmenity() {
            return amenity;
        }

        public void setAmenity(String amenity) {
            this.amenity = amenity;
        }

        public String getAtm() {
            return atm;
        }

        public void setAtm(String atm) {
            this.atm = atm;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getWheelchair() {
            return wheelchair;
        }

        public void setWheelchair(String wheelchair) {
            this.wheelchair = wheelchair;
        }

        public String getAddrCity() {
            return addrCity;
        }

        public void setAddrCity(String addrCity) {
            this.addrCity = addrCity;
        }

        public String getAddrHousenumber() {
            return addrHousenumber;
        }

        public void setAddrHousenumber(String addrHousenumber) {
            this.addrHousenumber = addrHousenumber;
        }

        public String getAddrPostcode() {
            return addrPostcode;
        }

        public void setAddrPostcode(String addrPostcode) {
            this.addrPostcode = addrPostcode;
        }

        public String getAddrStreet() {
            return addrStreet;
        }

        public void setAddrStreet(String addrStreet) {
            this.addrStreet = addrStreet;
        }

        public String getOpeningHours() {
            return openingHours;
        }

        public void setOpeningHours(String openingHours) {
            this.openingHours = openingHours;
        }

        public String getAddrCountry() {
            return addrCountry;
        }

        public void setAddrCountry(String addrCountry) {
            this.addrCountry = addrCountry;
        }

        public String getBuilding() {
            return building;
        }

        public void setBuilding(String building) {
            this.building = building;
        }

        public String getBuildingUse() {
            return buildingUse;
        }

        public void setBuildingUse(String buildingUse) {
            this.buildingUse = buildingUse;
        }

}
