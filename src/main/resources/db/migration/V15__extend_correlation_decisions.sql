do $$
begin
    if exists (
        select 1 from alert_groups g left join business_services s on s.id = g.probable_root_service_id
        where g.probable_root_service_id is null or s.id is null
    ) then
        raise exception 'Cannot migrate correlation decisions: an existing group has no valid root service';
    end if;
    if exists (
        select 1 from alerts a left join alert_groups g on g.id = a.group_id
        where a.group_id is not null and g.id is null
    ) then
        raise exception 'Cannot migrate correlation decisions: an alert references a missing group';
    end if;
end $$;

alter table alert_groups add column if not exists root_assessment jsonb;
alter table alert_groups add column if not exists root_assessment_status varchar(20);
alter table alerts add column if not exists link_strength varchar(20);
alter table alerts add column if not exists link_evidence jsonb;

update alert_groups set root_assessment = jsonb_build_object(
    'formatVersion', 1,
    'assessment', jsonb_build_object(
        'status', 'RANKED',
        'candidates', jsonb_build_array(jsonb_build_object('serviceId', probable_root_service_id, 'score', 1.0)),
        'evidence', jsonb_build_array(jsonb_build_object('type', 'MIGRATED'))
    )
), root_assessment_status = 'RANKED'
where root_assessment is null;

update alerts set link_strength = 'TENTATIVE',
    link_evidence = jsonb_build_object('formatVersion', 1,
        'items', jsonb_build_array(jsonb_build_object('type', 'MIGRATED')))
where group_id is not null and link_evidence is null;

alter table alert_groups alter column probable_root_service_id drop not null;
alter table alert_groups alter column root_assessment set not null;
alter table alert_groups alter column root_assessment_status set not null;
alter table alert_groups add constraint chk_alert_groups_root_assessment_status
    check (root_assessment_status in ('RANKED', 'UNDETERMINED'));
alter table alert_groups add constraint chk_alert_groups_root_assessment_root
    check ((root_assessment_status = 'UNDETERMINED') = (probable_root_service_id is null));
alter table alerts add constraint chk_alerts_link_strength
    check (link_strength in ('SUPPORTED', 'TENTATIVE'));
alter table alerts add constraint chk_alerts_link_metadata
    check ((group_id is null and link_strength is null and link_evidence is null)
        or (group_id is not null and link_evidence is not null));
