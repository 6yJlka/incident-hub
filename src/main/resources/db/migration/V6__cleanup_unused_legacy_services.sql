delete from business_services legacy_service
where lower(btrim(legacy_service.code)) in (
    'legacy_infrastructure',
    'legacy_application',
    'legacy_business_process'
)
and not exists (
    select 1
    from incidents incident
    where incident.affected_service_id = legacy_service.id
)
and not exists (
    select 1
    from service_dependencies dependency
    where dependency.dependent_id = legacy_service.id
       or dependency.dependency_id = legacy_service.id
);

delete from teams legacy_team
where lower(btrim(legacy_team.code)) = 'legacy_services'
and not exists (
    select 1
    from business_services business_service
    where business_service.owner_team_id = legacy_team.id
)
and not exists (
    select 1
    from incidents incident
    where incident.responsible_team_id = legacy_team.id
);
