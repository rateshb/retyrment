package com.retyrment.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuth2RedirectStateStore {

    private static final long TTL_MILLIS = 5 * 60 * 1000;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void put(String state, String origin) {
        if (state == null || state.isBlank() || origin == null || origin.isBlank()) {
            return;
        }
        cleanup();
        store.put(state, new Entry(origin, System.currentTimeMillis()));
    }

    public String getAndRemove(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        cleanup();
        Entry entry = store.remove(state);
        if (entry == null) {
            return null;
        }
        return entry.origin;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> (now - entry.getValue().createdAt) > TTL_MILLIS);
    }

    private static final class Entry {
        private final String origin;
        private final long createdAt;

        private Entry(String origin, long createdAt) {
            this.origin = origin;
            this.createdAt = createdAt;
        }
    }
}
