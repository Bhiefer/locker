package cz.evans.locker.service.dto;

public record ServiceOpenResult(
        Result result,
        Integer boxNo,
        String reason
) {
    public enum Result { OK, DENY }
}
