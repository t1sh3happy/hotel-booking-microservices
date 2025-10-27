package com.example.booking.web;

import com.example.booking.model.Booking;
import com.example.booking.model.CreateBookingRequest;  // ✅ ДОБАВИТЬ IMPORT
import com.example.booking.repo.BookingRepository;
import com.example.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
public class BookingController {
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public BookingController(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping
    public Booking create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateBookingRequest req) {
        Long userId = Long.parseLong(jwt.getSubject());
        Long roomId = req.getRoomId();  // ✅ ИЗМЕНЕНО
        LocalDate start = req.getStartDate();  // ✅ ИЗМЕНЕНО
        LocalDate end = req.getEndDate();  // ✅ ИЗМЕНЕНО
        String requestId = req.getRequestId();  // ✅ ИЗМЕНЕНО
        boolean autoSelect = req.isAutoSelect();  // ✅ НОВОЕ

        return bookingService.createBooking(userId, roomId, start, end, requestId, autoSelect);
    }

    @GetMapping
    public List<Booking> myBookings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bookingRepository.findByUserId(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Booking>> all(@AuthenticationPrincipal Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if ("ADMIN".equals(scope)) {
            return ResponseEntity.ok(bookingRepository.findAll());
        }
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBooking(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        String scope = jwt.getClaimAsString("scope");

        return bookingRepository.findById(id)
                .map(booking -> {
                    if (booking.getUserId().equals(userId) || "ADMIN".equals(scope)) {
                        return ResponseEntity.ok(booking);
                    }
                    return ResponseEntity.status(403).<Booking>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        String scope = jwt.getClaimAsString("scope");

        return bookingRepository.findById(id)
                .map(booking -> {
                    if (booking.getUserId().equals(userId) || "ADMIN".equals(scope)) {
                        bookingService.cancelBooking(booking);
                        return ResponseEntity.ok().<Void>build();
                    }
                    return ResponseEntity.status(403).<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
