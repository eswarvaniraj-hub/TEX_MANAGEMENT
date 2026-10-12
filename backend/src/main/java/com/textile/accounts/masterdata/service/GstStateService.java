package com.textile.accounts.masterdata.service;

import com.textile.accounts.masterdata.model.GstState;
import com.textile.accounts.masterdata.repository.GstStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GstStateService {

    private final GstStateRepository gstStateRepository;

    public GstStateService(GstStateRepository gstStateRepository) {
        this.gstStateRepository = gstStateRepository;
    }

    public List<GstState> getAllStates() {
        return gstStateRepository.findAllByOrderByCodeAsc();
    }
}
