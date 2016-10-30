select count(*) 
from (
	select distinct ClickURL
	from SAG_WEDT
	)
;
