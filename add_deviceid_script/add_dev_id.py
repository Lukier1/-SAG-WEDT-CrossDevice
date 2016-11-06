import datetime
import numpy
import math
import random

from operator import itemgetter

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, DateTime, String
from sqlalchemy import func
from sqlalchemy import distinct


Base = declarative_base()

class AOLRow(Base):
	__tablename__ = 'AOL'
	
	AnonId = Column(Integer, primary_key=True)
	Query = Column(String, primary_key=True)
	QueryTime = Column(DateTime, primary_key=True)
	ItemRank = Column(Integer, primary_key=True)
	ClickURL = Column(String, primary_key=True)
	
	def __init__(self, anonid, q, qt, rank, url):
		self.AnonId = anonid
		self.Query = q
		self.QueryTime = qt
		self.ItemRank = rank
		self.ClickURL = url


def get_aol_rows(session, anonid):
	rows = session.query(AOLRow).filter(AOLRow.AnonId == anonid)\
	.order_by(AOLRow.QueryTime).all()
	return rows


class AOLDevicesRow(Base):
	__tablename__ = 'AOL_devices'
	
	DeviceId = Column(Integer, primary_key=True)
	AnonId = Column(Integer, primary_key=True)
	Query = Column(String, primary_key=True)
	QueryTime = Column(DateTime, primary_key=True)
	ItemRank = Column(Integer, primary_key=True)
	ClickURL = Column(String, primary_key=True)
	
	def __init__(self, devid, anonid, q, qt, rank, url):
		self.DeviceId = devid
		self.AnonId = anonid
		self.Query = q
		self.QueryTime = qt
		self.ItemRank = rank
		self.ClickURL = url


def put_aol_dev_rows(session, rows, bulk_size):
	i = 0
	for r in rows:
		session.add(r)
		i = i+1
		if i % bulk_size == 0:
			session.commit()
	session.commit


def draw_devices_num(sessions_count):
	if  6 >= sessions_count >= 2:
		return numpy.random.choice(numpy.arange(1, 3), p=[0.2, 0.8])
	elif sessions_count > 6:
		return numpy.random.choice(numpy.arange(1, 4), p=[0.1, 0.2, 0.7])
	return 1


def time_delta_minutes(datetime1, datetime2):
	delta = datetime1 - datetime2
	seconds = delta.total_seconds()
	minutes = seconds // 60
	return minutes


def get_query_sessions(queries):
	sessions = []
	count = 1
	session = [queries[0]]
	while count < len(queries):
		last_time = session[len(session)-1].QueryTime
		current_time = queries[count].QueryTime
		delta_time = time_delta_minutes(current_time, last_time)
		if delta_time <= 30:
			session.append(queries[count])
		else:
			ratio = float(len(session)) / len(queries)
			sessions.append((ratio, session))
			session = [queries[count]]
		count += 1
	if len(sessions) == 0:
		ratio = float(len(session)) / len(queries)
		sessions.append((ratio, session))
	sorted_sessions = sorted(sessions, key=itemgetter(0), reverse=True)
	return sorted_sessions


# 'sessions_ratios' needs to be sorted in desceding order with values 1-100:
def indexes_of_minimum_number_of_sessions_with_given_sum(sessions_ratios, sum_threshold):
	indexes = []
	curr_sum = 100
	for i in range(len(sessions_ratios)):
		if sessions_ratios[i] >= sum_threshold:
			if sessions_ratios[i] < curr_sum:
				curr_sum = sessions_ratios[i]
				indexes = [i]
	if len(indexes) > 0:
		return indexes
	curr_sum = 100
	indexes[:] = []
	for i in range(len(sessions_ratios)-1):
		for j in range(i+1, len(sessions_ratios)):
			s = sum(sessions_ratios[:i+1]) + sessions_ratios[j]
			if s >= sum_threshold:
				if s < curr_sum:
					curr_sum = s
					indexes[:] = []
					indexes = range(i+1) + [j]
	if len(indexes) > 0:
		return indexes
	return range(len(sessions_ratios))


def add_devids_to_rows(query_sessions, dev_count, queries_count, current_devid):
	dev_queries = []
	devid = current_devid
	
	if dev_count == 1:
		for session in query_sessions:
			for q in session[1]:
				devq = AOLDevicesRow(devid, q.AnonId, q.Query, q.QueryTime, q.ItemRank, q.ClickURL)
				dev_queries.append(devq)
		return dev_queries
	
	sessions_ratios = [s[0]*100 for s in query_sessions]
	
	# desktop (dev1): 60%, mobile (dev2): 40%
	if dev_count == 2:
		# dev 1:
		inds = indexes_of_minimum_number_of_sessions_with_given_sum(sessions_ratios, 60)
		for i in inds:
			for q in query_sessions[i][1]:
				devq = AOLDevicesRow(devid, q.AnonId, q.Query, q.QueryTime, q.ItemRank, q.ClickURL)
				dev_queries.append(devq)
		# dev 2 (if there are sessions left):
		if len(inds) < len(query_sessions):
			devid += 1
			for i in range(len(query_sessions)):
				if i not in inds:
					for q in query_sessions[i][1]:
						devq = AOLDevicesRow(devid, q.AnonId, q.Query, q.QueryTime, q.ItemRank, q.ClickURL)
						dev_queries.append(devq)
		return dev_queries
	
	# desktop (dev1): 50%, mobile (dev2): 30%, tablet (dev3): 20%
	if dev_count == 3:
		inds2 = None
		# dev 1:
		inds1 = indexes_of_minimum_number_of_sessions_with_given_sum(sessions_ratios, 50)
		for i in inds1:
			for q in query_sessions[i][1]:
				devq = AOLDevicesRow(devid, q.AnonId, q.Query, q.QueryTime, q.ItemRank, q.ClickURL)
				dev_queries.append(devq)
		# dev 2 (if there are sessions left):
		if len(inds1) < len(query_sessions):
			devid += 1
			new_ratios = [0]*len(query_sessions)
			for i in range(len(new_ratios)):
				if i not in inds1:
					new_ratios[i] = sessions_ratios[i]
			inds2 = indexes_of_minimum_number_of_sessions_with_given_sum(new_ratios, 30)
			inds2 = list(set(inds2)-set(inds1))
			for i in inds2:
				for q in query_sessions[i][1]:
					devq = AOLDevicesRow(devid, q.AnonId, q.Query, q.QueryTime, q.ItemRank, q.ClickURL)
					dev_queries.append(devq)
		# dev 3 (if there are sessions left):
		if inds2 is not None and len(inds2)+len(inds1) < len(query_sessions):
			devid += 1
			for i in range(len(query_sessions)):
				if i not in inds1 and i not in inds2:
					for q in query_sessions[i][1]:
						devq = AOLDevicesRow(devid, q.AnonId, q.Query, q.QueryTime, q.ItemRank, q.ClickURL)
						dev_queries.append(devq)
		return dev_queries


if __name__ == "__main__":
	source_db_engine = create_engine('sqlite:///original_db/aol.db', echo=False)
	Session = sessionmaker(bind=source_db_engine)
	src_session = Session()
	
	anonids_result = src_session.query(AOLRow.AnonId).distinct().all()
	anonids = [x[0] for x in anonids_result]
	
	dest_db_engine = create_engine('sqlite:///dest_db/aol_devices.db', echo=False)
	Session = sessionmaker(bind=dest_db_engine)
	dest_session = Session()
	
	print "Db sessions created. Script started."
	
	current_devid = 1
	#to iterate through anonids from indexes <N, M):
	#for current_anonid in anonids[N:M]:
	for current_anonid in anonids:
		queries = get_aol_rows(src_session, current_anonid)
		if queries is None or len(queries) <= 0:
			continue
		ss = get_query_sessions(queries)
		dev_count = draw_devices_num(len(ss))
		dev_queries = add_devids_to_rows(ss, dev_count, len(queries), current_devid)
		put_aol_dev_rows(dest_session, dev_queries, 300)
		current_devid += dev_count

	print "Done."
