insert into teams (name, code, active, created_at, updated_at)
values
    ('Platform Engineering', 'PLATFORM', true, current_timestamp - interval '180 days', current_timestamp - interval '30 days'),
    ('Identity & Access', 'IDENTITY', true, current_timestamp - interval '170 days', current_timestamp - interval '20 days'),
    ('Payments Platform', 'PAYMENTS', true, current_timestamp - interval '160 days', current_timestamp - interval '10 days'),
    ('Customer Operations', 'CUSTOMER_OPS', true, current_timestamp - interval '150 days', current_timestamp - interval '5 days');

insert into users (email, display_name, active, created_at, updated_at)
values
    ('anna.ivanova@incidenthub.demo', 'Anna Ivanova', true, current_timestamp - interval '140 days', current_timestamp - interval '12 days'),
    ('boris.petrov@incidenthub.demo', 'Boris Petrov', true, current_timestamp - interval '130 days', current_timestamp - interval '8 days'),
    ('elena.sokolova@incidenthub.demo', 'Elena Sokolova', true, current_timestamp - interval '120 days', current_timestamp - interval '6 days'),
    ('maksim.kuznetsov@incidenthub.demo', 'Maksim Kuznetsov', true, current_timestamp - interval '110 days', current_timestamp - interval '4 days'),
    ('olga.smirnova@incidenthub.demo', 'Olga Smirnova', true, current_timestamp - interval '100 days', current_timestamp - interval '2 days'),
    ('sergey.volkov@incidenthub.demo', 'Sergey Volkov', false, current_timestamp - interval '200 days', current_timestamp - interval '45 days');

insert into business_services (
    code, name, description, owner_team_id, tier, active, created_at, updated_at
)
values
    (
        'CORE_DATABASE', 'Core Database', 'Primary PostgreSQL cluster for transactional application data',
        (select id from teams where code = 'PLATFORM'), 'TIER_1', true,
        current_timestamp - interval '145 days', current_timestamp - interval '14 days'
    ),
    (
        'EVENT_BUS', 'Event Bus', 'Kafka event backbone for asynchronous service communication',
        (select id from teams where code = 'PLATFORM'), 'TIER_1', true,
        current_timestamp - interval '142 days', current_timestamp - interval '13 days'
    ),
    (
        'IDENTITY_API', 'Identity API', 'Authentication and identity verification API',
        (select id from teams where code = 'IDENTITY'), 'TIER_1', true,
        current_timestamp - interval '138 days', current_timestamp - interval '11 days'
    ),
    (
        'API_GATEWAY', 'API Gateway', 'External API entry point with routing and request policies',
        (select id from teams where code = 'PLATFORM'), 'TIER_1', true,
        current_timestamp - interval '135 days', current_timestamp - interval '9 days'
    ),
    (
        'PAYMENT_API', 'Payment API', 'Card authorization and payment orchestration service',
        (select id from teams where code = 'PAYMENTS'), 'TIER_1', true,
        current_timestamp - interval '132 days', current_timestamp - interval '7 days'
    ),
    (
        'BILLING_WORKER', 'Billing Worker', 'Asynchronous settlement and billing event processor',
        (select id from teams where code = 'PAYMENTS'), 'TIER_2', true,
        current_timestamp - interval '128 days', current_timestamp - interval '6 days'
    ),
    (
        'NOTIFICATION_SERVICE', 'Notification Service', 'Customer email and push notification delivery',
        (select id from teams where code = 'CUSTOMER_OPS'), 'TIER_2', true,
        current_timestamp - interval '124 days', current_timestamp - interval '5 days'
    ),
    (
        'CUSTOMER_PORTAL', 'Customer Portal', 'Customer-facing web application for account and payment management',
        (select id from teams where code = 'CUSTOMER_OPS'), 'TIER_2', true,
        current_timestamp - interval '120 days', current_timestamp - interval '4 days'
    ),
    (
        'SUPPORT_DESK', 'Support Desk', 'Internal tooling for customer support specialists',
        (select id from teams where code = 'CUSTOMER_OPS'), 'TIER_3', true,
        current_timestamp - interval '116 days', current_timestamp - interval '3 days'
    ),
    (
        'ANALYTICS', 'Operations Analytics', 'Operational dashboards built from billing events',
        (select id from teams where code = 'CUSTOMER_OPS'), 'TIER_3', true,
        current_timestamp - interval '112 days', current_timestamp - interval '2 days'
    );

insert into service_dependencies (dependent_id, dependency_id, type, created_at)
values
    ((select id from business_services where code = 'IDENTITY_API'), (select id from business_services where code = 'CORE_DATABASE'), 'DATA', current_timestamp - interval '100 days'),
    ((select id from business_services where code = 'PAYMENT_API'), (select id from business_services where code = 'CORE_DATABASE'), 'DATA', current_timestamp - interval '99 days'),
    ((select id from business_services where code = 'API_GATEWAY'), (select id from business_services where code = 'IDENTITY_API'), 'SYNC', current_timestamp - interval '98 days'),
    ((select id from business_services where code = 'CUSTOMER_PORTAL'), (select id from business_services where code = 'API_GATEWAY'), 'SYNC', current_timestamp - interval '97 days'),
    ((select id from business_services where code = 'CUSTOMER_PORTAL'), (select id from business_services where code = 'PAYMENT_API'), 'SYNC', current_timestamp - interval '96 days'),
    ((select id from business_services where code = 'SUPPORT_DESK'), (select id from business_services where code = 'CUSTOMER_PORTAL'), 'SYNC', current_timestamp - interval '95 days'),
    ((select id from business_services where code = 'BILLING_WORKER'), (select id from business_services where code = 'PAYMENT_API'), 'ASYNC', current_timestamp - interval '94 days'),
    ((select id from business_services where code = 'BILLING_WORKER'), (select id from business_services where code = 'EVENT_BUS'), 'ASYNC', current_timestamp - interval '93 days'),
    ((select id from business_services where code = 'NOTIFICATION_SERVICE'), (select id from business_services where code = 'EVENT_BUS'), 'ASYNC', current_timestamp - interval '92 days'),
    ((select id from business_services where code = 'ANALYTICS'), (select id from business_services where code = 'BILLING_WORKER'), 'DATA', current_timestamp - interval '91 days');

insert into incidents (
    title, description, affected_service_id, source, priority, severity, status,
    reporter_id, responsible_team_id, assignee_id, created_at, updated_at
)
values
    (
        'Primary database connection saturation', 'Connection pool usage remains above 95 percent on the primary cluster.',
        (select id from business_services where code = 'CORE_DATABASE'), 'AUTOMATIC', 'CRITICAL', 'SEV1', 'IN_PROGRESS',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'PLATFORM'),
        (select id from users where email = 'boris.petrov@incidenthub.demo'),
        current_timestamp - interval '6 hours', current_timestamp - interval '5 hours 45 minutes'
    ),
    (
        'Elevated authentication latency', 'The p95 latency of token validation exceeds the service objective.',
        (select id from business_services where code = 'IDENTITY_API'), 'AUTOMATIC', 'HIGH', 'SEV2', 'ASSIGNED',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'IDENTITY'),
        (select id from users where email = 'elena.sokolova@incidenthub.demo'),
        current_timestamp - interval '4 hours', current_timestamp - interval '3 hours 55 minutes'
    ),
    (
        'Intermittent login failures', 'A subset of customers received invalid session errors during sign-in.',
        (select id from business_services where code = 'IDENTITY_API'), 'MANUAL', 'CRITICAL', 'SEV2', 'RESOLVED',
        (select id from users where email = 'olga.smirnova@incidenthub.demo'),
        (select id from teams where code = 'IDENTITY'),
        (select id from users where email = 'elena.sokolova@incidenthub.demo'),
        current_timestamp - interval '3 days', current_timestamp - interval '2 days 23 hours'
    ),
    (
        'API gateway timeout spike', 'Gateway requests timed out while an upstream route was overloaded.',
        (select id from business_services where code = 'API_GATEWAY'), 'AUTOMATIC', 'HIGH', 'SEV2', 'CLOSED',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'PLATFORM'),
        (select id from users where email = 'boris.petrov@incidenthub.demo'),
        current_timestamp - interval '5 days', current_timestamp - interval '4 days 22 hours 30 minutes'
    ),
    (
        'Card payment authorization failures', 'Payment authorization requests fail for one acquiring bank.',
        (select id from business_services where code = 'PAYMENT_API'), 'MANUAL', 'CRITICAL', 'SEV1', 'OPEN',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'PAYMENTS'), null,
        current_timestamp - interval '15 minutes', current_timestamp - interval '15 minutes'
    ),
    (
        'Billing event backlog reopened', 'The backlog returned after the initial consumer recovery.',
        (select id from business_services where code = 'BILLING_WORKER'), 'AUTOMATIC', 'HIGH', 'SEV2', 'IN_PROGRESS',
        (select id from users where email = 'olga.smirnova@incidenthub.demo'),
        (select id from teams where code = 'PAYMENTS'),
        (select id from users where email = 'maksim.kuznetsov@incidenthub.demo'),
        current_timestamp - interval '2 days', current_timestamp - interval '1 day 22 hours 45 minutes'
    ),
    (
        'Delayed customer notifications', 'Transactional email delivery is delayed by approximately twenty minutes.',
        (select id from business_services where code = 'NOTIFICATION_SERVICE'), 'AUTOMATIC', 'MEDIUM', 'SEV3', 'ASSIGNED',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'CUSTOMER_OPS'),
        (select id from users where email = 'olga.smirnova@incidenthub.demo'),
        current_timestamp - interval '2 hours', current_timestamp - interval '1 hour 55 minutes'
    ),
    (
        'Customer portal degraded', 'Customers experienced slow page loads during the morning traffic peak.',
        (select id from business_services where code = 'CUSTOMER_PORTAL'), 'MANUAL', 'HIGH', 'SEV2', 'CLOSED',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'CUSTOMER_OPS'),
        (select id from users where email = 'olga.smirnova@incidenthub.demo'),
        current_timestamp - interval '7 days', current_timestamp - interval '6 days 22 hours 30 minutes'
    ),
    (
        'Support desk attachment errors', 'Support specialists cannot attach diagnostic files to customer cases.',
        (select id from business_services where code = 'SUPPORT_DESK'), 'MANUAL', 'LOW', 'SEV4', 'OPEN',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'CUSTOMER_OPS'), null,
        current_timestamp - interval '30 minutes', current_timestamp - interval '30 minutes'
    ),
    (
        'Analytics dashboard data is stale', 'Operations dashboards have not received new billing metrics.',
        (select id from business_services where code = 'ANALYTICS'), 'MANUAL', 'MEDIUM', 'SEV3', 'RESOLVED',
        (select id from users where email = 'olga.smirnova@incidenthub.demo'),
        (select id from teams where code = 'CUSTOMER_OPS'),
        (select id from users where email = 'maksim.kuznetsov@incidenthub.demo'),
        current_timestamp - interval '1 day', current_timestamp - interval '23 hours'
    ),
    (
        'Event bus partition imbalance', 'A transient partition imbalance cleared before intervention was required.',
        (select id from business_services where code = 'EVENT_BUS'), 'AUTOMATIC', 'HIGH', 'SEV3', 'CANCELLED',
        (select id from users where email = 'anna.ivanova@incidenthub.demo'),
        (select id from teams where code = 'PLATFORM'), null,
        current_timestamp - interval '8 hours', current_timestamp - interval '7 hours 50 minutes'
    ),
    (
        'Scheduled billing reconciliation delay', 'A reconciliation batch exceeded its planned processing window.',
        (select id from business_services where code = 'BILLING_WORKER'), 'MANUAL', 'LOW', 'SEV4', 'CLOSED',
        (select id from users where email = 'olga.smirnova@incidenthub.demo'),
        (select id from teams where code = 'PAYMENTS'),
        (select id from users where email = 'maksim.kuznetsov@incidenthub.demo'),
        current_timestamp - interval '10 days', current_timestamp - interval '9 days 22 hours 30 minutes'
    );

insert into incident_audit_events (incident_id, event_type, from_status, to_status, created_at)
select incident.id, 'CREATED', null, 'OPEN', incident.created_at
from incidents incident
join business_services service on service.id = incident.affected_service_id
where service.code in (
    'CORE_DATABASE', 'IDENTITY_API', 'API_GATEWAY', 'PAYMENT_API', 'BILLING_WORKER',
    'NOTIFICATION_SERVICE', 'CUSTOMER_PORTAL', 'SUPPORT_DESK', 'ANALYTICS', 'EVENT_BUS'
)
and incident.title in (
    'Primary database connection saturation',
    'Elevated authentication latency',
    'Intermittent login failures',
    'API gateway timeout spike',
    'Card payment authorization failures',
    'Billing event backlog reopened',
    'Delayed customer notifications',
    'Customer portal degraded',
    'Support desk attachment errors',
    'Analytics dashboard data is stale',
    'Event bus partition imbalance',
    'Scheduled billing reconciliation delay'
);

insert into incident_audit_events (incident_id, event_type, from_status, to_status, created_at)
select incident.id, event.event_type, event.from_status, event.to_status, incident.created_at + event.elapsed
from (
    values
        ('Primary database connection saturation', 'CORE_DATABASE', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Primary database connection saturation', 'CORE_DATABASE', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('Elevated authentication latency', 'IDENTITY_API', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Intermittent login failures', 'IDENTITY_API', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Intermittent login failures', 'IDENTITY_API', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('Intermittent login failures', 'IDENTITY_API', 'RESOLVED', 'IN_PROGRESS', 'RESOLVED', interval '1 hour'),
        ('API gateway timeout spike', 'API_GATEWAY', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('API gateway timeout spike', 'API_GATEWAY', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('API gateway timeout spike', 'API_GATEWAY', 'RESOLVED', 'IN_PROGRESS', 'RESOLVED', interval '1 hour'),
        ('API gateway timeout spike', 'API_GATEWAY', 'CLOSED', 'RESOLVED', 'CLOSED', interval '1 hour 30 minutes'),
        ('Billing event backlog reopened', 'BILLING_WORKER', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Billing event backlog reopened', 'BILLING_WORKER', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('Billing event backlog reopened', 'BILLING_WORKER', 'RESOLVED', 'IN_PROGRESS', 'RESOLVED', interval '1 hour'),
        ('Billing event backlog reopened', 'BILLING_WORKER', 'REOPENED', 'RESOLVED', 'IN_PROGRESS', interval '1 hour 15 minutes'),
        ('Delayed customer notifications', 'NOTIFICATION_SERVICE', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Customer portal degraded', 'CUSTOMER_PORTAL', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Customer portal degraded', 'CUSTOMER_PORTAL', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('Customer portal degraded', 'CUSTOMER_PORTAL', 'RESOLVED', 'IN_PROGRESS', 'RESOLVED', interval '1 hour'),
        ('Customer portal degraded', 'CUSTOMER_PORTAL', 'CLOSED', 'RESOLVED', 'CLOSED', interval '1 hour 30 minutes'),
        ('Analytics dashboard data is stale', 'ANALYTICS', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Analytics dashboard data is stale', 'ANALYTICS', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('Analytics dashboard data is stale', 'ANALYTICS', 'RESOLVED', 'IN_PROGRESS', 'RESOLVED', interval '1 hour'),
        ('Event bus partition imbalance', 'EVENT_BUS', 'CANCELLED', 'OPEN', 'CANCELLED', interval '10 minutes'),
        ('Scheduled billing reconciliation delay', 'BILLING_WORKER', 'ASSIGNED', 'OPEN', 'ASSIGNED', interval '5 minutes'),
        ('Scheduled billing reconciliation delay', 'BILLING_WORKER', 'STARTED', 'ASSIGNED', 'IN_PROGRESS', interval '15 minutes'),
        ('Scheduled billing reconciliation delay', 'BILLING_WORKER', 'RESOLVED', 'IN_PROGRESS', 'RESOLVED', interval '1 hour'),
        ('Scheduled billing reconciliation delay', 'BILLING_WORKER', 'CLOSED', 'RESOLVED', 'CLOSED', interval '1 hour 30 minutes')
) as event(title, service_code, event_type, from_status, to_status, elapsed)
join business_services service on service.code = event.service_code
join incidents incident on incident.title = event.title and incident.affected_service_id = service.id;
