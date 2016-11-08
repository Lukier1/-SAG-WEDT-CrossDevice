if [ ! -d ".venv" ]; then
  mkdir .venv
  virtualenv .venv
fi

source .venv/bin/activate

#importing libs
pip install numpy
pip install sqlalchemy
####################

python "add_deviceid_script/add_dev_id.py"
