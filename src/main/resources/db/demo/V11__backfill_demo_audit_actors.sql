update incident_audit_events event
set actor_id = case
    when event.event_type = 'CREATED' then incident.reporter_id
    else (
        select admin.id
        from users admin
        where admin.role = 'ADMIN'
        order by admin.id
        limit 1
    )
end
from incidents incident
where event.incident_id = incident.id
  and event.actor_id is null;
