package cz.evans.locker.service.dto;

public record OpenResult(
        Result result,
        Integer boxNo,
        String reason,
        Integer remainingAttempts
) {
    public enum Result { OK, DENY }
}
