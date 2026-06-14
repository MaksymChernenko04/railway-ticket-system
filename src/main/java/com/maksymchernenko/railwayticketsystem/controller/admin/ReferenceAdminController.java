package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.reference.ReferenceCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.reference.ReferenceResponse;
import com.maksymchernenko.railwayticketsystem.service.reference.ReferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/references")
@RequiredArgsConstructor
public class ReferenceAdminController {

    private final ReferenceService referenceService;

    @GetMapping("/{type}")
    public Page<ReferenceResponse> getReferences(@PathVariable String type, Pageable pageable) {
        return referenceService.getReferences(type, pageable);
    }

    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReferenceResponse createReference(
            @PathVariable String type,
            @Valid @RequestBody ReferenceCreateRequest request
    ) {
        return referenceService.createReference(type, request);
    }

    @DeleteMapping("/{type}/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReference(@PathVariable String type, @PathVariable String code) {
        referenceService.deleteReference(type, code);
    }
}
