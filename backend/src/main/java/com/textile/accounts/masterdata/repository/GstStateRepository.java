package com.textile.accounts.masterdata.repository;

import com.textile.accounts.masterdata.model.GstState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GstStateRepository extends JpaRepository<GstState, String> {
    List<GstState> findAllByOrderByCodeAsc();
}
