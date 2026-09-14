update users
set password_hash = '$2a$10$jc8olO3ze8GSjmE.mX3DC.Fv2HQSCoAq7wJKe0w/EzFf.nnFBFY/u',
    role = case email
        when 'anna.ivanova@incidenthub.demo' then 'REPORTER'
        when 'boris.petrov@incidenthub.demo' then 'ENGINEER'
        when 'elena.sokolova@incidenthub.demo' then 'ENGINEER'
        when 'maksim.kuznetsov@incidenthub.demo' then 'ENGINEER'
        when 'olga.smirnova@incidenthub.demo' then 'ADMIN'
        when 'sergey.volkov@incidenthub.demo' then 'REPORTER'
        else role
    end
where email like '%@incidenthub.demo';
