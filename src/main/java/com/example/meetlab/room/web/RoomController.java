package com.example.meetlab.room.web;

import com.example.meetlab.room.domain.Room;
import com.example.meetlab.room.service.RoomService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.create(request.name(), request.maxParticipants());
        URI location = URI.create("/api/rooms/" + room.getCode());
        return ResponseEntity.created(location).body(RoomResponse.from(room));
    }

    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable String code) {
        return RoomResponse.from(roomService.get(code));
    }

    @GetMapping
    public List<RoomResponse> recent() {
        return roomService.recent().stream().map(RoomResponse::from).toList();
    }
}
