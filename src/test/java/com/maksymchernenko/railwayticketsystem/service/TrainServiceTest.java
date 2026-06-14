package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.TrainCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TrainResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
import com.maksymchernenko.railwayticketsystem.repository.railway.TrainRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.TrainTypeRepository;
import com.maksymchernenko.railwayticketsystem.service.railway.TrainServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TrainServiceTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private TrainTypeRepository trainTypeRepository;

    @InjectMocks
    private TrainServiceImpl trainService;

    @Test
    void getAllTrainsShouldMapEntitiesToResponses() {
        Pageable pageable = PageRequest.of(0, 2);

        TrainType trainType = new TrainType();
        trainType.setCode("INTERCITY");

        Train train = new Train();
        train.setId(1L);
        train.setType(trainType);
        train.setNumber("625");
        train.setName("Anubis");

        when(trainRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(train), pageable, 1));

        Page<TrainResponse> result = trainService.getAllTrains(pageable);

        assertThat(result.getContent())
                .containsExactly(new TrainResponse(1L, "INTERCITY", "625", "Anubis"));
    }

    @Test
    void createTrainShouldTrimValuesNormalizeTypeCodeAndReturnResponse() {
        TrainCreateRequest trainCreateRequest = new TrainCreateRequest(
                "  intercity  ",
                "  625  ",
                "  Anubis  "
        );

        TrainType trainType = new TrainType();
        trainType.setCode("INTERCITY");

        Train savedTrain = new Train();
        savedTrain.setId(1L);
        savedTrain.setType(trainType);
        savedTrain.setNumber("625");
        savedTrain.setName("Anubis");

        when(trainRepository.existsByNumberIgnoreCase("625")).thenReturn(false);
        when(trainTypeRepository.findById("INTERCITY")).thenReturn(Optional.of(trainType));
        when(trainRepository.save(any(Train.class))).thenReturn(savedTrain);

        TrainResponse result = trainService.createTrain(trainCreateRequest);

        ArgumentCaptor<Train> trainCaptor = ArgumentCaptor.forClass(Train.class);
        verify(trainRepository).save(trainCaptor.capture());
        Train trainToSave = trainCaptor.getValue();

        assertThat(trainToSave.getNumber()).isEqualTo("625");
        assertThat(trainToSave.getName()).isEqualTo("Anubis");
        assertThat(trainToSave.getType().getCode()).isEqualTo("INTERCITY");
        assertThat(result).isEqualTo(new TrainResponse(
                1L,
                "INTERCITY",
                "625",
                "Anubis"
        ));
    }

    @Test
    void createTrainShouldThrowWhenTrainAlreadyExists() {
        TrainCreateRequest request = new TrainCreateRequest(
                "INTERCITY",
                "625",
                "Anubis"
        );

        when(trainRepository.existsByNumberIgnoreCase("625")).thenReturn(true);

        assertThatThrownBy(() -> trainService.createTrain(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Train with this number already exists");

        verify(trainTypeRepository, never()).findById(any());
        verify(trainRepository, never()).save(any());
    }

    @Test
    void createTrainShouldThrowWhenTypeDoesNotExist() {
        TrainCreateRequest request = new TrainCreateRequest(
                "INTERCITY",
                "625",
                "Anubis"
        );

        when(trainRepository.existsByNumberIgnoreCase("625")).thenReturn(false);
        when(trainTypeRepository.findById("INTERCITY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainService.createTrain(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Train type not found: INTERCITY");

        verify(trainRepository, never()).save(any());
    }
}
