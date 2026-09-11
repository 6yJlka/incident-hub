package ru.donskikh.incidenthub.catalog.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyService;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceService;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesService;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceService;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesService;
import ru.donskikh.incidenthub.catalog.application.RemoveServiceDependencyService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/services")
public class BusinessServiceController {

    private static final String DEFAULT_MAX_DEPTH = "10";

    private final CreateBusinessServiceService createBusinessServiceService;
    private final ListBusinessServicesService listBusinessServicesService;
    private final GetBusinessServiceService getBusinessServiceService;
    private final GetAffectedServicesService getAffectedServicesService;
    private final AddServiceDependencyService addServiceDependencyService;
    private final RemoveServiceDependencyService removeServiceDependencyService;
    private final BusinessServiceWebMapper mapper;

    public BusinessServiceController(
            CreateBusinessServiceService createBusinessServiceService,
            ListBusinessServicesService listBusinessServicesService,
            GetBusinessServiceService getBusinessServiceService,
            GetAffectedServicesService getAffectedServicesService,
            AddServiceDependencyService addServiceDependencyService,
            RemoveServiceDependencyService removeServiceDependencyService,
            BusinessServiceWebMapper mapper
    ) {
        this.createBusinessServiceService = createBusinessServiceService;
        this.listBusinessServicesService = listBusinessServicesService;
        this.getBusinessServiceService = getBusinessServiceService;
        this.getAffectedServicesService = getAffectedServicesService;
        this.addServiceDependencyService = addServiceDependencyService;
        this.removeServiceDependencyService = removeServiceDependencyService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<CreateBusinessServiceResponse> create(
            @Valid @RequestBody CreateBusinessServiceRequest request
    ) {
        CreateBusinessServiceResponse response = mapper.toResponse(
                createBusinessServiceService.create(mapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.businessServiceId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ListBusinessServicesResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ownerTeamId,
            @RequestParam(required = false) ServiceTier tier,
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(listBusinessServicesService.execute(
                mapper.toQuery(page, size, ownerTeamId, tier, active)
        ));
    }

    @GetMapping("/{id}")
    public BusinessServiceResponse get(@PathVariable long id) {
        return mapper.toResponse(getBusinessServiceService.get(mapper.toGetQuery(id)));
    }

    @GetMapping("/{id}/affected")
    public GetAffectedServicesResponse affected(
            @PathVariable long id,
            @RequestParam(defaultValue = DEFAULT_MAX_DEPTH) int maxDepth
    ) {
        return mapper.toResponse(getAffectedServicesService.get(
                mapper.toAffectedQuery(id, maxDepth)
        ));
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<AddServiceDependencyResponse> addDependency(
            @PathVariable long id,
            @Valid @RequestBody AddServiceDependencyRequest request
    ) {
        AddServiceDependencyResponse response = mapper.toResponse(
                addServiceDependencyService.add(mapper.toCommand(id, request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/dependencies/{dependencyId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable long id,
            @PathVariable long dependencyId
    ) {
        removeServiceDependencyService.remove(mapper.toRemoveCommand(id, dependencyId));
        return ResponseEntity.noContent().build();
    }
}
