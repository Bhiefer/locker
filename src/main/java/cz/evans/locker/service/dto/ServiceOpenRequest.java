package cz.evans.locker.service.dto;

public record ServiceOpenRequest(
        String locationCode,
        String servicePin,
        int boxNo
) {}
