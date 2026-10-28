package com.example.meetlab.room.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, String> {

    List<Room> findTop10ByOrderByCreatedAtDesc();
}
