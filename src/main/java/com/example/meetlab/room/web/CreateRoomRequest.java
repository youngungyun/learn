package com.example.meetlab.room.web;

import com.example.meetlab.room.domain.Room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(max = Room.MAX_NAME_LENGTH) String name,
        @Min(Room.MIN_PARTICIPANTS) @Max(Room.MAX_PARTICIPANTS) Integer maxParticipants) {
}
