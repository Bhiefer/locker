package cz.evans.locker.service.dto;

public record OpenRequest(
        String deviceId,
        long wooOrderId,
        String pin
) {}
