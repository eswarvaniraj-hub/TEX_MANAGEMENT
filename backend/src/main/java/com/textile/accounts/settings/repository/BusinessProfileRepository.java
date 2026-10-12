package com.textile.accounts.settings.repository;

import com.textile.accounts.settings.model.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    default Optional<BusinessProfile> getProfile() {
        return findById(1L);
    }
}
