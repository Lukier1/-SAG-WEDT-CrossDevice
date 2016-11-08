#!/bin/bash
mkdir -p ../db
touch ../db/aol_devices.db
bunzip2 -c aol_devices.sql.bz2 | sqlite3 ../db/aol_devices.db
