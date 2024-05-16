package ru.rozhdestveno.taxi.user;

public enum UserStatus {
    CLIENT,
    DRIVER,
    ADMIN,
    DISPATCHER;

    public static void checkUserStatus(String status) {
        int isPresent = 0;
        for (UserStatus value : UserStatus.values()) {
             if (status.equals(value.name())) {
                 isPresent++;
             }
        }
        if (isPresent != 1) {
            throw new RuntimeException("Unknown status: " + status);
        }
    }
}