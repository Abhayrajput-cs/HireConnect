package com.hireconnect.notification.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.notification.dto.NotificationEvent;
import com.hireconnect.notification.dto.NotificationResponse;
import com.hireconnect.notification.service.OfferLetterAttachment;
import com.hireconnect.notification.service.NotificationService;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationResource {

    private final NotificationService notificationService;

    public NotificationResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> publishEvent(@RequestBody NotificationEvent event) {
        notificationService.sendNotification(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getByUser(
        @PathVariable Integer userId,
        @RequestParam(required = false) Boolean isRead
    ) {
        return ResponseEntity.ok(notificationService.getByUser(userId, isRead));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllRead(@PathVariable Integer userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @GetMapping("/offer-letters/{applicationId}")
    public ResponseEntity<byte[]> downloadOfferLetter(@PathVariable Integer applicationId) {
        OfferLetterAttachment offerLetter = notificationService.buildOfferLetterForApplication(applicationId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(offerLetter.filename()).build().toString()
            )
            .body(offerLetter.content());
    }
}
