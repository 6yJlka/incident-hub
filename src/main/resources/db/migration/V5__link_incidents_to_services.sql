alter table incidents
    add column affected_service_id bigint;

insert into teams (name, code, active)
select 'Legacy Services', 'LEGACY_SERVICES', true
where not exists (select 1 from teams);

insert into business_services (
    code, name, description, owner_team_id, tier, active
)
select
    legacy.code,
    legacy.name,
    legacy.description,
    (select min(id) from teams),
    'TIER_3',
    true
from (
    values
        (
            'LEGACY_INFRASTRUCTURE',
            'Legacy Infrastructure',
            'Service created from the legacy INFRASTRUCTURE incident category'
        ),
        (
            'LEGACY_APPLICATION',
            'Legacy Application',
            'Service created from the legacy APPLICATION incident category'
        ),
        (
            'LEGACY_BUSINESS_PROCESS',
            'Legacy Business Process',
            'Service created from the legacy BUSINESS_PROCESS incident category'
        )
) as legacy(code, name, description)
where not exists (
    select 1
    from business_services existing_service
    where lower(btrim(existing_service.code)) = lower(btrim(legacy.code))
);

update incidents incident
set affected_service_id = business_service.id
from (
    values
        ('INFRASTRUCTURE', 'LEGACY_INFRASTRUCTURE'),
        ('APPLICATION', 'LEGACY_APPLICATION'),
        ('BUSINESS_PROCESS', 'LEGACY_BUSINESS_PROCESS')
) as legacy(category_value, service_code)
join business_services business_service
    on lower(btrim(business_service.code)) = lower(btrim(legacy.service_code))
where incident.category = legacy.category_value;

do $$
declare
    unmapped_categories text;
begin
    select string_agg(distinct category, ', ' order by category)
    into unmapped_categories
    from incidents
    where affected_service_id is null;

    if unmapped_categories is not null then
        raise exception
            'Cannot link incidents to legacy services. Unmapped categories: %',
            unmapped_categories;
    end if;
end
$$;

alter table incidents
    alter column affected_service_id set not null;

alter table incidents
    add constraint fk_incidents_affected_service
        foreign key (affected_service_id) references business_services (id);

create index idx_incidents_affected_service_id
    on incidents (affected_service_id);

alter table incidents
    drop constraint chk_incidents_category;

alter table incidents
    drop column category;

alter table incidents
    add column severity varchar(10);

update incidents
set severity = case priority
    when 'CRITICAL' then 'SEV1'
    when 'HIGH' then 'SEV2'
    when 'MEDIUM' then 'SEV3'
    when 'LOW' then 'SEV4'
end;

do $$
begin
    if exists (
        select 1
        from incidents
        where severity is null
    ) then
        raise exception
            'Cannot assign severity to all incidents. Check existing priority values.';
    end if;
end
$$;

alter table incidents
    alter column severity set not null;

alter table incidents
    add constraint chk_incidents_severity
        check (severity in ('SEV1', 'SEV2', 'SEV3', 'SEV4'));
