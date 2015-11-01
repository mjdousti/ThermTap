#!/system/bin/sh
#
#
# Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
#   and Massoud Pedram, SPORT lab, University of Southern California. All 
#   rights reserved.
# 
# Please refer to the LICENSE file for terms of use.
#
#
echo "Inserting the SystemTap module"

RUN_DIR="/data/local/systemtap"
MODULE_NAME="powertap.ko"
STAP_DIR="/data/local/systemtap"

export SYSTEMTAP_STAPRUN=$STAP_DIR"/staprun"
export SYSTEMTAP_STAPIO=$STAP_DIR"/stapio"

chmod 755 $SYSTEMTAP_STAPRUN $SYSTEMTAP_STAPIO
$SYSTEMTAP_STAPRUN -L $RUN_DIR"/"$MODULE_NAME &

