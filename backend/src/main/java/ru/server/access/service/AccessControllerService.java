package ru.server.access.service;

import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.server.access.dto.ControllerDtos.*;
import ru.server.access.entity.*;
import ru.server.access.exception.*;
import ru.server.access.repository.*;

import java.util.List;
import java.util.Set;

@Service
public class AccessControllerService {

    private static final Set<String> READER_TYPES = Set.of(
            "Wiegand",
            "Barcode_terminator",
            "Barcode-USB_terminator",
            "Barcode",
            "Barcode-USB"
    );

    private final AccessControllerRepository controllerRepository;
    private final ReaderRepository readerRepository;
    private final PercoSocketService socketService;

    public AccessControllerService(
            AccessControllerRepository controllerRepository,
            ReaderRepository readerRepository,
            PercoSocketService socketService
    ) {
        this.controllerRepository = controllerRepository;
        this.readerRepository = readerRepository;
        this.socketService = socketService;
    }

    @Transactional(readOnly = true)
    public List<ControllerResponse> findAll() {
        return controllerRepository.findAll().stream()
                .map(this::mapController)
                .toList();
    }

    @Transactional
    public ControllerResponse create(
            CreateControllerRequest request
    ) {
        String ip = request.ip().trim();

        if (controllerRepository.existsByIp(ip)) {
            throw new BadRequestException(
                    "Контроллер с таким IP уже существует"
            );
        }

        AccessController controller = new AccessController();
        controller.setName(request.name().trim());
        controller.setIp(ip);
        controller.setWebSocketUrl(normalize(request.webSocketUrl()));
        controller.setControllerPassword(request.password());
        controller.setEnabled(true);
        controller.setConnected(false);
        controller.setAuthenticated(false);

        return mapController(controllerRepository.save(controller));
    }

    @Transactional
    public void delete(Long id) {
        AccessController controller = findController(id);
        controllerRepository.delete(controller);
    }

    @Transactional(readOnly = true)
    public List<ReaderResponse> findReaders(Long controllerId) {
        findController(controllerId);

        return readerRepository
                .findAllByControllerIdOrderByNumber(controllerId)
                .stream()
                .map(this::mapReader)
                .toList();
    }

    @Transactional
    public ReaderResponse createReader(
            Long controllerId,
            CreateReaderRequest request
    ) {
        AccessController controller = findController(controllerId);

        if (!READER_TYPES.contains(request.type())) {
            throw new BadRequestException(
                    "Неизвестный тип считывателя"
            );
        }

        if (readerRepository.existsByControllerIdAndNumber(
                controllerId,
                request.number()
        )) {
            throw new BadRequestException(
                    "Считыватель с таким номером уже существует"
            );
        }

        Reader reader = new Reader();
        reader.setController(controller);
        reader.setNumber(request.number());
        reader.setName(request.name().trim());
        reader.setType(request.type());
        reader.setPort(request.port());
        reader.setExdevNumber(request.exdevNumber());
        reader.setExdevDirection(request.exdevDirection());

        reader = readerRepository.save(reader);

        if (socketService.isConnected(controllerId)) {
            socketService.send(
                    controllerId,
                    createSetReaderCommand(reader)
            );
        }

        return mapReader(reader);
    }

    @Transactional
    public void deleteReader(
            Long controllerId,
            Long readerId
    ) {
        findController(controllerId);

        Reader reader = readerRepository.findById(readerId)
                .orElseThrow(() ->
                        new NotFoundException("Считыватель не найден")
                );

        if (!reader.getController().getId().equals(controllerId)) {
            throw new BadRequestException(
                    "Считыватель не принадлежит контроллеру"
            );
        }

        if (socketService.isConnected(controllerId)) {
            JsonObject readerObject = new JsonObject();
            readerObject.addProperty("number", reader.getNumber());

            JsonObject command = new JsonObject();
            command.addProperty("set", "reader");
            command.add("reader", readerObject);

            socketService.send(controllerId, command);
        }

        readerRepository.delete(reader);
    }

    private JsonObject createSetReaderCommand(Reader reader) {
        JsonObject readerObject = new JsonObject();
        readerObject.addProperty("number", reader.getNumber());
        readerObject.addProperty("type", reader.getType());
        readerObject.addProperty("port", reader.getPort());
        readerObject.addProperty(
                "exdev_number",
                reader.getExdevNumber()
        );
        readerObject.addProperty(
                "exdev_direction",
                reader.getExdevDirection()
        );

        JsonObject command = new JsonObject();
        command.addProperty("set", "reader");
        command.add("reader", readerObject);

        return command;
    }

    private AccessController findController(Long id) {
        return controllerRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Контроллер не найден")
                );
    }

    private ControllerResponse mapController(
            AccessController controller
    ) {
        return new ControllerResponse(
                controller.getId(),
                controller.getName(),
                controller.getIp(),
                controller.getWebSocketUrl(),
                controller.isEnabled(),
                controller.isConnected(),
                controller.isAuthenticated(),
                controller.getLastSeen() == null
                        ? null
                        : controller.getLastSeen().toString()
        );
    }

    private ReaderResponse mapReader(Reader reader) {
        return new ReaderResponse(
                reader.getId(),
                reader.getNumber(),
                reader.getName(),
                reader.getType(),
                reader.getPort(),
                reader.getExdevNumber(),
                reader.getExdevDirection()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}