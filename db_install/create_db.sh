#!/bin/bash
touch aol_devices.db
bunzip2 -c aol_devices.sql.bz2 | sqlite3 aol_devices.db
