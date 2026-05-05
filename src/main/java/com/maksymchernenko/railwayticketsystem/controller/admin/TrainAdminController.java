package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.railway.TrainCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TrainResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.TypeResponse;
import com.maksymchernenko.railwayticketsystem.service.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/trains")
@RequiredArgsConstructor
public class TrainAdminController {

    private final TrainService trainService;

    @GetMapping
    public Page<TrainResponse> getTrains(Pageable pageable) {
        return trainService.getAllTrains(pageable);
    }

    @GetMapping("/types")
    public List<TypeResponse> getTrainTypes() {
        return trainService.getTrainTypes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainResponse createTrain(@Valid @RequestBody TrainCreateRequest request) {
        return trainService.createTrain(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrain(@PathVariable Long id) {
        trainService.deleteTrain(id);
    }
}
