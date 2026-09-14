alter table incident_audit_events
    add column actor_id bigint;

alter table incident_audit_events
    add constraint fk_incident_audit_events_actor
        foreign key (actor_id) references users (id);

create index idx_incident_audit_events_actor_id
    on incident_audit_events (actor_id);
