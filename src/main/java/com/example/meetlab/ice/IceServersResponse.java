package com.example.meetlab.ice;

import java.time.Instant;
import java.util.List;

public record IceServersResponse(List<IceServer> iceServers, Instant expiresAt) {
}
