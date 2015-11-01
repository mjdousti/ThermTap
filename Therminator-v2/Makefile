#
#
# Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, 
#    SPORT lab, University of Southern California. All rights reserved.
# 
# Please refer to the LICENSE file for terms of use.
#
#
VPATH = src
CXX = g++

INCLUDE = -I/usr/include/eigen3 -I${MKLROOT}/include
LIBRARIES = ${MKLROOT}/lib/intel64/libmkl_intel_lp64.a ${MKLROOT}/lib/intel64/libmkl_core.a ${MKLROOT}/lib/intel64/libmkl_intel_thread.a

CXXFLAGS = -O3 -std=c++11 -Wno-unused-local-typedefs ${INCLUDE} -Wall -D USE_GPU=0  -m64 -fopenmp
LDFLAGS = -O3 ${LIBRARIES}  -Wall  -Wl,--start-group -Wl,--end-group -liomp5 -ldl -lpthread -lm -lrt

TARGET = therminator

OBJ = src/libs/inotify-cxx/inotify-cxx.o src/libs/pugixml/pugixml.o therminator.o component.o device.o entity.o floorplan.o material.o model.o subcomponent.o parser.o physical_entity.o rc_utils.o utils.o

$(TARGET) : $(OBJ)
	$(CXX) $(OBJ) $(LDFLAGS) -o $(TARGET)
clean:
	rm -f $(TARGET) $(TARGET).exe $(OBJ)
