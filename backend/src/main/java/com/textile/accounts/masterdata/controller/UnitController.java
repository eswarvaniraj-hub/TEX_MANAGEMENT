package com.textile.accounts.masterdata.controller;

import com.textile.accounts.masterdata.dto.MasterDataDtos.UnitRequest;
import com.textile.accounts.masterdata.dto.MasterDataDtos.UnitResponse;
import com.textile.accounts.masterdata.service.UnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public ResponseEntity<List<UnitResponse>> getAllUnits(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(unitService.getAllUnits(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UnitResponse> create(
            @Valid @RequestBody UnitRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(unitService.createUnit(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UnitRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(unitService.updateUnit(id, request, version, actor));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<UnitResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(unitService.setUnitActive(id, active, actor));
    }
}
