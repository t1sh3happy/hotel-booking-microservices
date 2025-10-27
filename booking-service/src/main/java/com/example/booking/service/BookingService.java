package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.repo.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final WebClient webClient;
    private final String hotelBaseUrl;
    private final int retries;
    private final Duration timeout;

    public BookingService(
            BookingRepository bookingRepository,
            WebClient.Builder builder,
            @Value("${hotel.base-url}") String hotelBaseUrl,
            @Value("${hotel.timeout-ms}") int timeoutMs,
            @Value("${hotel.retries}") int retries
    ) {
        this.bookingRepository = bookingRepository;
        this.webClient = builder.baseUrl(hotelBaseUrl).build();
        this.hotelBaseUrl = hotelBaseUrl;
        this.retries = retries;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    @Transactional
    public Booking createBooking(Long userId, Long roomId, LocalDate start, LocalDate end, String requestId, boolean autoSelect) {
        String correlationId = UUID.randomUUID().toString();

        Booking existing = bookingRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return existing;
        }

        if (autoSelect) {
            log.info("[{}] Auto-selecting room", correlationId);
            roomId = selectBestAvailableRoom(correlationId);
            log.info("[{}] Auto-selected room ID: {}", correlationId, roomId);
        } else {
            // Ручной выбор номера
            if (roomId == null) {
                throw new IllegalArgumentException("Room ID is required when autoSelect is false");
            }
            log.info("[{}] Using manually selected room ID: {}", correlationId, roomId);
        }

        Booking booking = new Booking();
        booking.setRequestId(requestId);
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setStatus(Booking.Status.PENDING);
        booking.setCorrelationId(correlationId);
        booking.setCreatedAt(java.time.OffsetDateTime.now());
        booking = bookingRepository.save(booking);

        log.info("[{}] Booking PENDING created for room {}", correlationId, roomId);

        Map<String, String> payload = Map.of(
                "requestId", requestId,
                "startDate", start.toString(),
                "endDate", end.toString()
        );

        try {
            // Удержание слота (hold)
            callHotel("/rooms/" + roomId + "/hold", payload, correlationId).block(timeout);
            // Подтверждение (confirm)
            callHotel("/rooms/" + roomId + "/confirm", Map.of("requestId", requestId), correlationId).block(timeout);
            booking.setStatus(Booking.Status.CONFIRMED);
            bookingRepository.save(booking);
            log.info("[{}] Booking CONFIRMED", correlationId);
        } catch (Exception e) {
            log.warn("[{}] Booking flow failed: {}", correlationId, e.toString());
            // Компенсация (release) при ошибке
            try { callHotel("/rooms/" + roomId + "/release", Map.of("requestId", requestId), correlationId).block(timeout); } catch (Exception ignored) {}
            booking.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(booking);
            log.info("[{}] Booking CANCELLED and compensated", correlationId);
        }

        return booking;
    }

    private Long selectBestAvailableRoom(String correlationId) {
        try {
            List<RoomView> rooms = getRoomSuggestions().block(timeout);

            if (rooms == null || rooms.isEmpty()) {
                log.error("[{}] No available rooms found", correlationId);
                throw new IllegalStateException("No available rooms for the selected dates");
            }

            // Возвращаем первый номер (уже отсортирован по timesBooked)
            RoomView selectedRoom = rooms.get(0);
            log.info("[{}] Selected room {} with {} bookings",
                    correlationId, selectedRoom.id(), selectedRoom.timesBooked());

            return selectedRoom.id();

        } catch (Exception e) {
            log.error("[{}] Error fetching recommended rooms: {}", correlationId, e.getMessage());
            throw new IllegalStateException("Unable to auto-select room: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelBooking(Booking booking) {
        String correlationId = UUID.randomUUID().toString();

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            log.warn("[{}] Cannot cancel booking {} with status {}", correlationId, booking.getId(), booking.getStatus());
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }

        log.info("[{}] Cancelling booking {}", correlationId, booking.getId());

        try {
            callHotel("/rooms/" + booking.getRoomId() + "/release",
                    Map.of("requestId", booking.getRequestId()),
                    correlationId)
                    .block(timeout);
            log.info("[{}] Successfully released room {} for booking {}",
                    correlationId, booking.getRoomId(), booking.getId());
        } catch (Exception e) {
            log.error("[{}] Failed to release room: {}", correlationId, e.getMessage());
        }

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);

        log.info("[{}] Booking {} cancelled", correlationId, booking.getId());
    }

    private Mono<String> callHotel(String path, Map<String, String> payload, String correlationId) {
        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .header("X-Correlation-Id", correlationId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(retries, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(2)));
    }

    public record RoomView(Long id, String number, long timesBooked) {}

    public Mono<java.util.List<RoomView>> getRoomSuggestions() {
        return webClient.get()
                .uri("/hotels/rooms")
                .retrieve()
                .bodyToFlux(RoomView.class)
                .collectList()
                .map(list -> list.stream()
                        .sorted(java.util.Comparator.comparingLong(RoomView::timesBooked)
                                .thenComparing(RoomView::id))
                        .toList());
    }
}
