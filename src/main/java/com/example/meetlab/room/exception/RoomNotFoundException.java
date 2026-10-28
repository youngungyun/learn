package com.example.meetlab.room.exception;

public class RoomNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoomNotFoundException(String code) {
        super("방을 찾을 수 없습니다. code=" + code);
    }
}
