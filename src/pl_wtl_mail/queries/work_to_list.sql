with dates as (
  select
    (trunc(sysdate, 'iw') + 6 + level) d
  from dual
  connect by level < 15
),
date_range as (
  select
     min(d) as start_date,
     max(d) as finish_date
  from dates
),
period as (
  select
    ('Week ' || w) as name,
    min(d) as start_date,
    max(d) as finish_date
  from (
    select
      (start_date + level - 1) d,
      ceil(level / 7) w
    from date_range
    connect by level < 29
  )
  group by
    w
),
shop_order_current_op as (
  select
    so.order_no,
    so.release_no,
    so.sequence_no,
    soo.operation_no,
    --
    soo.work_center_no,
    wc.description as work_center,
    --
    wc.production_line as production_line_id,
    pl.description as production_line,
    --
    case soo.oper_status_code
      when 'Planned' then 'Unstarted'
      when 'Released' then 'Unstarted'
      when 'Closed' then 'Closed'
      else 'Started'
    end as status
  from ifsapp.shop_ord so
  --
  join ifsapp.shop_order_operation soo
    on so.order_no = soo.order_no
    and so.release_no = soo.release_no
    and so.sequence_no = soo.sequence_no
    and soo.operation_no = ifsapp.lpe_shop_ord_util_api.earliest_op_with_os_qty(
      so.order_no,
      so.release_no,
      so.sequence_no
    )
  --
  join ifsapp.work_center wc
    on so.contract = wc.contract
    and soo.work_center_no = wc.work_center_no
  left outer join ifsapp.production_line pl
    on wc.contract = pl.contract
    and wc.production_line = pl.production_line
  --
  where so.close_date is null
),
ops as (
  select
    soo.order_no,
    soo.release_no,
    soo.sequence_no,
    soo.operation_no,
    soo.work_center_no,
    (
      row_number() over (
        partition by
          soo.order_no,
          soo.release_no,
          soo.sequence_no
        order by
          soo.operation_no
      )
      - row_number() over (
        partition by
          soo.order_no,
          soo.release_no,
          soo.sequence_no,
          soo.work_center_no
        order by
          soo.operation_no
      )
    ) so_wc_grp,
    -- run times
    ifsapp.lpe_routed_op_util_api.batch_run_time(
      soo.run_time_code_db,
      soo.mach_run_factor,
      (soo.revised_qty_due - soo.qty_complete - soo.qty_scrapped)
    ) as mach_run_time_remaining,
    ifsapp.lpe_routed_op_util_api.batch_run_time(
      soo.run_time_code_db,
      soo.labor_run_factor,
      (soo.revised_qty_due - soo.qty_complete - soo.qty_scrapped)
    ) as labor_run_time_remaining,
    --
    case soo.oper_status_code
      when 'Planned' then 'Unstarted'
      when 'Released' then 'Unstarted'
      when 'Closed' then 'Closed'
      else 'Started'
    end as status,
    -- scheduled dates
    nvl(
      lag(soo.op_finish_date) over (
        partition by
          soo.order_no,
          soo.release_no,
          soo.sequence_no
        order by
          soo.operation_no
      ),
      so.revised_start_date
    ) as op_start_date,
    soo.op_finish_date,
    -- qtys
    nvl(
      lag(soo.qty_complete) over (
        partition by
          soo.order_no,
          soo.release_no,
          soo.sequence_no
        order by
          soo.operation_no
      ),
      soo.revised_qty_due
    ) as qty_available,
    (soo.revised_qty_due - soo.qty_complete - soo.qty_scrapped) as qty_os,
    -- prev op details
    lag(soo.operation_no) over (
      partition by
        soo.order_no,
        soo.release_no,
        soo.sequence_no
      order by
        soo.operation_no
    ) as prev_operation_no,
    lag(soo.work_center_no) over (
      partition by
        soo.order_no,
        soo.release_no,
        soo.sequence_no
      order by
        soo.operation_no
    ) as prev_work_center_no
  from ifsapp.shop_ord so
  join ifsapp.shop_order_operation soo
    on soo.order_no = so.order_no
    and soo.release_no = so.release_no
    and soo.sequence_no = so.sequence_no
  where so.close_date is null
),
operation_runs as (
  select
    ops.order_no,
    ops.release_no,
    ops.sequence_no,
    min(ops.operation_no) as first_op_no,
    max(ops.operation_no) as last_op_no,
    rtrim(
      extract(
        xmlagg(xmlelement("X", ops.operation_no || ',') order by ops.operation_no),
        '/X/text()'
      ),
      ','
    ) op_sequence,
    -- run times
    sum(ops.mach_run_time_remaining) as mach_run_time_remaining,
    sum(ops.labor_run_time_remaining) as labor_run_time_remaining,
    max(ops.status) keep (dense_rank first order by ops.operation_no) as status,
    --
    min(ops.op_start_date) keep (dense_rank first order by ops.operation_no) as op_start_date,
    max(ops.op_finish_date) keep (dense_rank last order by ops.operation_no) as op_finish_date,
    max(ops.qty_available) keep (dense_rank first order by ops.operation_no) as qty_available,
    max(ops.prev_operation_no) keep (dense_rank first order by ops.operation_no) as prev_operation_no,
    max(ops.prev_work_center_no) keep (dense_rank first order by ops.operation_no) as prev_work_center_no
  from ops
  where ops.status <> 'Closed'
    and ops.qty_os > 0
  group by
    ops.order_no,
    ops.release_no,
    ops.sequence_no,
    ops.work_center_no,
    ops.so_wc_grp
),
work_to_list as (
  select
    opr.order_no,
    opr.release_no,
    opr.sequence_no,
    opr.op_sequence as ops,
    -- dates
    opr.op_start_date,
    opr.op_finish_date,
    -- quantities
    fop.revised_qty_due,
    fop.qty_complete,
    fop.qty_scrapped,
    (fop.revised_qty_due - fop.qty_complete - fop.qty_scrapped) as qty_remaining,
    greatest(opr.qty_available - fop.qty_complete - fop.qty_scrapped, 0) as qty_available,
    -- run times
    opr.mach_run_time_remaining,
    opr.labor_run_time_remaining,
    -- part details
    so.part_no as ifs_part_no,
    ipcp.cust_part_no as part_no,
    ipcp.issue as part_rev,
    ipcp.description as part_description,
    -- where is the shop order now/was it last?
    pop.operation_no as prior_op_no,
    prior_wc.work_center_no as prior_work_center_no,
    prior_wc.description as prior_work_center,
    prior_wc.production_line as prior_production_line_id,
    prior_pl.description as prior_production_line,
    -- work center
    fop.work_center_no,
    wc.description as work_center,
    -- production line
    wc.production_line as production_line_id,
    pl.description as production_line,
    -- categorization
    bp.value_no as buffer_penetration,
    so.priority_category as dbr_zone,
    case
      when opr.op_start_date < dr.start_date then
        'Past'
      else
        nvl(start_period.name, 'Future')
    end as to_start,
    case
      when opr.op_finish_date < dr.start_date then
        'Overdue'
      else
        nvl(finish_period.name, 'Future')
    end as due,
    case
      when opr.first_op_no = soco.operation_no then
        'Yes'
      when (opr.qty_available - fop.qty_complete - fop.qty_scrapped) > 0 then
        'Partially'
      else
        ''
    end available,
    case
      when opr.first_op_no = soco.operation_no then
        null
      else soco.status
    end as current_op_status,
    opr.status as op_status,
    ip.planner_buyer as planner
  from operation_runs opr
  --
  join ifsapp.shop_order_operation fop
    on opr.order_no = fop.order_no
    and opr.release_no = fop.release_no
    and opr.sequence_no = fop.sequence_no
    and opr.first_op_no = fop.operation_no
  --
  join shop_order_current_op soco
    on opr.order_no = soco.order_no
    and opr.release_no = soco.release_no
    and opr.sequence_no = soco.sequence_no
  --
  left outer join ifsapp.shop_order_operation pop
    on opr.order_no = pop.order_no
    and opr.release_no = pop.release_no
    and opr.sequence_no = pop.sequence_no
    and pop.operation_no = case
      when soco.operation_no = fop.operation_no then
        opr.prev_operation_no
      else
        soco.operation_no
    end
  left outer join ifsapp.work_center prior_wc
    on pop.contract = prior_wc.contract
    and pop.work_center_no = prior_wc.work_center_no
  left outer join ifsapp.production_line prior_pl
    on prior_wc.contract = prior_pl.contract
    and prior_wc.production_line = prior_pl.production_line
  --
  join ifsapp.shop_ord so
    on opr.order_no = so.order_no
    and opr.release_no = so.release_no
    and opr.sequence_no = so.sequence_no
  --
  join ifsapp.inventory_part ip
    on so.contract = ip.contract
    and so.part_no = ip.part_no
  join ifsinfo.inv_part_cust_part_no ipcp
    on so.part_no = ipcp.part_no
  --
  join ifsapp.work_center wc
    on fop.contract = wc.contract
    and fop.work_center_no = wc.work_center_no
  join ifsapp.production_line pl
    on wc.contract = pl.contract
    and wc.production_line = pl.production_line
  --
  join ifsapp.technical_object_reference so_tor
    on so_tor.lu_name = 'ShopOrd'
    and so_tor.key_value = (
      opr.order_no || '^' ||
      opr.release_no || '^' ||
      opr.sequence_no || '^'
    )
  join ifsapp.technical_spec_numeric bp
    on so_tor.technical_spec_no = bp.technical_spec_no
    and bp.attribute = 'DBR_BUFFER_PEN'
  --
  left outer join period start_period
    on opr.op_start_date between start_period.start_date and start_period.finish_date
  left outer join period finish_period
    on opr.op_finish_date between finish_period.start_date and finish_period.finish_date
  --
  join date_range dr on 1=1
  where (
    -- scheduled inside date range
    opr.op_start_date between dr.start_date and dr.finish_date
    or opr.op_finish_date between dr.start_date and dr.finish_date
    or dr.start_date between opr.op_start_date and opr.op_finish_date
    or dr.finish_date between opr.op_start_date and opr.op_finish_date
    -- overdue
    or opr.op_finish_date < dr.start_date
  )
),
scored_work_to_list as (
  select
    decode(
      round(buffer_penetration, 4),
      0, 0,
      log(10, abs(buffer_penetration)) * sign(buffer_penetration)
    ) as penetration_score,
    decode(
      dbr_zone,
      'BLACK',  'A',
      'RED',    'B',
      'YELLOW', 'C',
      'GREEN',  'D',
      'BLUE',   'E',
      'F'
    ) as zone_score,
    decode(
      available,
      'Yes',       'A',
      'Partially', 'B',
      'X'
    ) as availability_score,
    decode(
      to_start,
      'Past',   'A',
      'Week 1', 'B',
      'Week 2', 'C',
      'E'
    ) as start_score,
    decode(
      due,
      'Overdue', 'A',
      'Week 1',  'B',
      'Week 2',  'C',
      'Week 3',  'D',
      'E'
    ) as due_score,
    case
      when to_start = 'Past' and available is null then
        'Y'
      else
        ' '
    end as replan_priority,
    wtl.*
  from work_to_list wtl
),
prioritised_work_to_list as (
  select
    (
      replan_priority
      || start_score
      || availability_score
      || due_score
      || zone_score
    ) as priority,
    wtl.*
  from scored_work_to_list wtl
)
select
  (
    order_no
    || '-' ||
    release_no
    || '-' ||
    sequence_no
  ) as order_no,
  ops,
  --
  op_start_date,
  op_finish_date,
  --
  revised_qty_due as order_qty,
  qty_scrapped,
  qty_available,
  greatest(mach_run_time_remaining, labor_run_time_remaining) as time_remaining,
  --
  part_no,
  part_rev,
  part_description,
  --
  prior_op_no,
  prior_work_center_no,
  prior_work_center,
  prior_production_line,
  work_center_no,
  --
  replace(replace(dbr_zone, '^', ''), '_', ' ') as dbr_zone,
  to_start,
  due,
  planner,
  --
  production_line_id,
  production_line,
  priority,
  replan_priority as offplan
from prioritised_work_to_list wtl
where production_line_id <> '18' -- exclude inspection, for now
order by
  production_line,
  priority,
  penetration_score desc
