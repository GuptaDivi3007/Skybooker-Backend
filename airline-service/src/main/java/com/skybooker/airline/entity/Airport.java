package com.skybooker.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "airports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_airport_iata_code", columnNames = "iata_code"),
                @UniqueConstraint(name = "uk_airport_icao_code", columnNames = "icao_code")
        }
)
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "airport_id", nullable = false, updatable = false)
    private String airportId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "iata_code", nullable = false, length = 3)
    private String iataCode;

    @Column(name = "icao_code", length = 4)
    private String icaoCode;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String country;

    private Double latitude;

    private Double longitude;

    @Column(length = 80)
    private String timezone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Airport() {
    }

    public String getAirportId() {
        return airportId;
    }

    public void setAirportId(String airportId) {
        this.airportId = airportId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIataCode() {
        return iataCode;
    }

    public void setIataCode(String iataCode) {
        this.iataCode = iataCode;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}