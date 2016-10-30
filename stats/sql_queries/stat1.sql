select total||' - '||(total+100), count(AnonID) as UserCount
from (
	select AnonID, ((count(AnonID)/100)*100) as total
	from SAG_WEDT
	group by AnonID
	)
group by total
order by total desc;
