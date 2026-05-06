package com.skybooker.airline.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "airlines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_airline_iata_code", columnNames = "iata_code"),
                @UniqueConstraint(name = "uk_airline_icao_code", columnNames = "icao_code")
        }
)
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "airline_id", nullable = false, updatable = false)
    private String airlineId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "iata_code", nullable = false, length = 3)
    private String iataCode;

    @Column(name = "icao_code", length = 4)
    private String icaoCode;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(length = 80)
    private String country;

    @Column(name = "contact_email", length = 120)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Airline() {
    }

    public String getAirlineId() {
        return airlineId;
    }

    public void setAirlineId(String airlineId) {
        this.airlineId = airlineId;
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}