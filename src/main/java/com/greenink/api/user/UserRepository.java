package com.greenink.api.user;

import java.util.Optional;
import java.util.Set;

public interface UserRepository {
    Optional<UserAccount> findById(String userId);
    Optional<UserAccount> findByIdentifier(String normalizedIdentifier);
    UserAccount create(String normalizedIdentifier, Set<String> roles);
    UserAccount save(UserAccount user);
    void deleteById(String userId);
}
