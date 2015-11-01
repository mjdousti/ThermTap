#
#
# Copyright (C) 2015 Mohammad Javad Dousti, Majid Ghasemi-Gol, Mahdi Nazemi, 
#    and Massoud Pedram, SPORT lab, University of Southern California. All 
#    rights reserved.
# 
# Please refer to the LICENSE file for terms of use.
#
#


TARGETS = therminator thermtap.jar adb_tools android

TARGET: ${TARGETS}

therminator:
	make -j -C Therminator-v2
	mv Therminator-v2/therminator .

thermtap.jar:
	ant -f PowerTap/build.xml
	mv PowerTap/thermtap.jar .

adb_tools:
	ln -s PowerTap/adb_tools adb_tools

android:
	ln -s PowerTap/android android

run:
	java -jar thermtap.jar
 
clean:
	rm -f ${TARGETS} PowerTrace.txt log.txt output_0 next trace
	rm -rf data
	make -C Therminator-v2 clean
	ant -f PowerTap/build.xml clean
