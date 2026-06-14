package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.vehicle.TypeResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TrainCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TrainResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TrainService {

    Page<TrainResponse> getAllTrains(Pageable pageable);

    List<TypeResponse> getTrainTypes();

    TrainResponse createTrain(TrainCreateRequest request);

    void deleteTrain(Long id);
}
