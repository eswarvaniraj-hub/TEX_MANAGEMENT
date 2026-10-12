package com.textile.accounts.masterdata.controller;

import com.textile.accounts.masterdata.model.GstState;
import com.textile.accounts.masterdata.service.GstStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gst-states")
public class GstStateController {

    private final GstStateService gstStateService;

    public GstStateController(GstStateService gstStateService) {
        this.gstStateService = gstStateService;
    }

    @GetMapping
    public ResponseEntity<List<GstState>> getAllStates() {
        return ResponseEntity.ok(gstStateService.getAllStates());
    }
}
