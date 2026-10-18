do $$
begin
    if exists (select 1 from alert_groups where status <> 'OPEN' or incident_id is not null) then
        raise exception 'Cannot add decision metadata: existing alert groups must be OPEN without incidents; reconcile legacy decisions first';
    end if;
end $$;

alter table alert_groups
    add column version bigint not null default 0,
    add column decided_by_id bigint,
    add column decided_at timestamptz,
    add column dismiss_reason varchar(2000),
    add constraint fk_alert_groups_decided_by foreign key (decided_by_id) references users(id),
    add constraint uk_alert_groups_incident unique (incident_id),
    add constraint chk_alert_groups_incident_status check (
        (status = 'CONFIRMED' and incident_id is not null)
        or (status in ('OPEN', 'DISMISSED') and incident_id is null)
    ),
    add constraint chk_alert_groups_decision check (
        (status = 'OPEN' and decided_by_id is null and decided_at is null and dismiss_reason is null)
        or (status = 'CONFIRMED' and decided_by_id is not null and decided_at is not null and dismiss_reason is null)
        or (status = 'DISMISSED' and decided_by_id is not null and decided_at is not null)
    );

create index idx_alert_groups_decided_by on alert_groups (decided_by_id);
