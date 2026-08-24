package ru.server.access.controller;


import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.server.access.dto.ControllerDtos.*;
import ru.server.access.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/controllers")
public class ControllerController {

    private final AccessControllerService controllerService;
    private final PercoSocketService socketService;

    public ControllerController(
            AccessControllerService controllerService,
            PercoSocketService socketService
    ) {
        this.controllerService = controllerService;
        this.socketService = socketService;
    }

    @GetMapping
    public List<ControllerResponse> findAll() {
        return controllerService.findAll();
    }

    @PostMapping
    public ControllerResponse create(
            @Valid @RequestBody CreateControllerRequest request
    ) {
        return controllerService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        controllerService.delete(id);
    }

    @PostMapping("/{id}/connect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void connect(@PathVariable Long id) {
        socketService.connect(id);
    }

    @PostMapping("/{id}/command")
    public CommandResponse sendCommand(
            @PathVariable Long id,
            @RequestBody JsonObject command
    ) {
        socketService.send(id, command);

        return new CommandResponse(true, command.toString());
    }

    @GetMapping("/{id}/readers")
    public List<ReaderResponse> findReaders(
            @PathVariable Long id
    ) {
        return controllerService.findReaders(id);
    }

    @PostMapping("/{id}/readers")
    public ReaderResponse createReader(
            @PathVariable Long id,
            @Valid @RequestBody CreateReaderRequest request
    ) {
        return controllerService.createReader(id, request);
    }

    @DeleteMapping("/{controllerId}/readers/{readerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReader(
            @PathVariable Long controllerId,
            @PathVariable Long readerId
    ) {
        controllerService.deleteReader(controllerId, readerId);
    }
}