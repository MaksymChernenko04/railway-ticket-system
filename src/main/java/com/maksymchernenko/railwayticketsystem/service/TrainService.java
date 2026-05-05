package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.TrainCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TrainResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
import com.maksymchernenko.railwayticketsystem.repository.railway.TrainRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.TrainTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainService {

    private final TrainRepository trainRepository;
    private final TrainTypeRepository trainTypeRepository;

    @Transactional(readOnly = true)
    public Page<TrainResponse> getAllTrains(Pageable pageable) {
        return trainRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public TrainResponse createTrain(TrainCreateRequest request) {
        String normalizedTypeCode = request.typeCode().trim().toUpperCase();
        String normalizedNumber = request.number().trim();

        if (trainRepository.existsByNumberIgnoreCase(normalizedNumber)) {
            throw new EntityAlreadyExistsException("Train with this number already exists");
        }

        TrainType trainType = trainTypeRepository.findById(normalizedTypeCode)
                .orElseThrow(() -> new EntityNotFoundException("Train type not found: " + normalizedTypeCode));

        Train train = new Train();
        train.setType(trainType);
        train.setNumber(normalizedNumber);
        train.setName(request.name().trim());

        return toResponse(trainRepository.save(train));
    }

    @Transactional
    public void deleteTrain(Long id) {
        trainRepository.deleteById(id);
    }

    private TrainResponse toResponse(Train train) {
        return new TrainResponse(
                train.getId(),
                train.getType().getCode(),
                train.getNumber(),
                train.getName()
        );
    }
}
