package com.skybooker.seat.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_flight_seat_number",
                        columnNames = {"flight_id", "seat_number"}
                )
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seat_id", nullable = false, updatable = false)
    private String seatId;

    @Column(name = "flight_id", nullable = false)
    private String flightId;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class", nullable = false, length = 30)
    private SeatClass seatClass;

    @Column(name = "seat_row", nullable = false)
    private Integer rowNumber;

    @Column(name = "seat_column", nullable = false, length = 5)
    private String columnLetter;

    @Column(name = "is_window", nullable = false)
    private boolean windowSeat;

    @Column(name = "is_aisle", nullable = false)
    private boolean aisleSeat;

    @Column(name = "has_extra_legroom", nullable = false)
    private boolean extraLegroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "price_multiplier", nullable = false)
    private Double priceMultiplier = 1.0;

    @Column(name = "held_by_user_id")
    private String heldByUserId;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Version
    private Long version;

    public Seat() {
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getColumnLetter() {
        return columnLetter;
    }

    public void setColumnLetter(String columnLetter) {
        this.columnLetter = columnLetter;
    }

    public boolean isWindowSeat() {
        return windowSeat;
    }

    public void setWindowSeat(boolean windowSeat) {
        this.windowSeat = windowSeat;
    }

    public boolean isAisleSeat() {
        return aisleSeat;
    }

    public void setAisleSeat(boolean aisleSeat) {
        this.aisleSeat = aisleSeat;
    }

    public boolean isExtraLegroom() {
        return extraLegroom;
    }

    public void setExtraLegroom(boolean extraLegroom) {
        this.extraLegroom = extraLegroom;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public Double getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(Double priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public String getHeldByUserId() {
        return heldByUserId;
    }

    public void setHeldByUserId(String heldByUserId) {
        this.heldByUserId = heldByUserId;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}