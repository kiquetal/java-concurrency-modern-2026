package dev.concurrency.sharingobjects;

import java.util.List;

public final class ImmutableUser {
    private final String name;
    private final List<String> roles;

    public ImmutableUser(String name, List<String> roles) {
        this.name = name;
        // Create an unmodifiable copy to ensure immutability
        this.roles = List.copyOf(roles);
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }
}
