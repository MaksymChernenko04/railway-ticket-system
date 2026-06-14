package com.maksymchernenko.railwayticketsystem.service.reference;

import com.maksymchernenko.railwayticketsystem.dto.reference.ReferenceCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.reference.ReferenceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReferenceService {

    Page<ReferenceResponse> getReferences(String type, Pageable pageable);

    ReferenceResponse createReference(String type, ReferenceCreateRequest request);

    void deleteReference(String type, String code);
}
