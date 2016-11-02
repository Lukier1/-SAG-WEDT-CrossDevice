select AnonId as user, (julianday(datetime(maxDate)) - julianday(datetime(minDate)))*24 as dateRangeHours
from(
select distinct AnonId,
(	select max(datetime(QueryTime)) 
	from SAG_WEDT T2
	where T1.AnonId = T2.AnonId
) as maxDate,
(	select min(datetime(QueryTime))
	from SAG_WEDT T3
	where T1.AnonId = T3.AnonId
) as minDate
from SAG_WEDT T1
) as T
order by user asc;
