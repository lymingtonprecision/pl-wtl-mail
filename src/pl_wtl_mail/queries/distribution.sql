-- name: -team-leader-production-lines
select
  ta.value_text as id,
  ifsapp.lpe_mail_api.user_address(ta.value_text) as email,
  ifsapp.client_sys.get_key_reference_value(tor.key_ref, 'PRODUCTION_LINE') as production_line
from ifsapp.technical_spec_alphanum ta
join ifsapp.technical_object_reference tor
  on ta.technical_spec_no = tor.technical_spec_no
where ta.technical_class = 'PRODLINE'
  and ta.attribute = 'PROD_TEAM_LEAD'
  and ta.value_text is not null
;

-- name: -supervisor-production-lines
select
  ta.value_text as id,
  ifsapp.lpe_mail_api.user_address(ta.value_text) as email,
  ifsapp.client_sys.get_key_reference_value(tor.key_ref, 'PRODUCTION_LINE') as production_line
from ifsapp.technical_spec_alphanum ta
join ifsapp.technical_object_reference tor
  on ta.technical_spec_no = tor.technical_spec_no
where ta.technical_class = 'PRODLINE'
  and ta.attribute = 'PROD_SUPERVISOR'
  and ta.value_text is not null
;