/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/

#ifndef MODEL_H_
#define MODEL_H_
#include "general.h"
#include "device.h"
#include "rc_utils.h"
#include "utils.h"

/* model specific constants */
/* changed from 1/2 to 1/3 due to the difference from traditional Elmore Delay scenario */
//#define C_FACTOR    0.33       /* fitting factor to match floworks (due to lumping)    */


class Model {
	Device *device;

	/* for sparse matrix calculation */
	int** a_matrix_indices;
	double** a_matrix_values;

	double** G_matrix,
			**A_matrix,	//A = C^-1 * G
			*invC_vector;	//Inverted form of diagonal matrix C stored as a vector
	double* amb_vector;	//This saves "device->getTemperature() * K1",
						//which comes from the lhs of the equation due to dropping the term corresponds to the thermal coupling to the ambient
	vector <double*> P_vector;
	vector < vector<double> > T_vector;
	bool P_vector_made, G_matrix_made, C_vector_made, T_vector_made;
	bool transient;
	int elements_no;
	map<string, int> powerMappingDeviceOrder;
	map<int, string> powerMappingTraceOrder;
	ifstream powerTraceFile;
	bool isComment(string s);
	int pwr_consumers_cnt;	//This designates the number of power consumers we expect to find in the power trace file
						//It is used to check if the power trace has enough info
	string powerTraceFileAddr;
	void preparePVector();
	void allocateNewPwrTempVector();
	void initPwrTempVector();

public:
	void makePVector();
	void makeResistanceModel();
	void makeCapacitanceModel();


	/**
	 * This function reads one line from the power trace file and place it
	 * in the power vector
	 *
	 * @return If the power trace file has any remaining line for reading
	 */
	int read_power();

	void solveSteadyState();
	void solveTransientState();
	void printSubComponentTemp();
	void printComponentTemp(string file_output);
	void printGMatrix();
	void printCVector();
	void printPVector();
	void printTVector();
	Model(Device * device, bool isTransient);
	virtual ~Model();
};

#endif
