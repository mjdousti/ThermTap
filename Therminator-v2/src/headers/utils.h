/*
 * 
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab, 
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/

#ifndef UTILS_H_
#define UTILS_H_
#include "general.h"

//OpenMP header file
#include <omp.h>


#include "../libs/inotify-cxx/inotify-cxx.h"
#define INOTIFY_THREAD_SAFE

class Utils {
public:
	static double KtoC(double temp);
	static bool isDouble(string const& s);
	static void matrixCopy (double **dst, double **src, int row_no, int col_no);
	static float** matrixFloatAlloc (int row_no, int col_no);
	static double** matrixAlloc (int row_no, int col_no);
	static int **matrixIntAlloc (int row_no, int col_no);
	static double* vectorAlloc (int items);

	static void matrixDealloc (float **matrix, int row_no);
	static void matrixDealloc (double **matrix, int row_no);
	static void matrixDealloc (int **matrix, int row_no);
	
	static void vectorDealloc (float *vector);
	static void vectorDealloc (double *vector);
	static void dumpMatrix (double **matrix, int row_no, int col_no);
	static void dumpVector (double *vector, int row_no);
	static void dumpVector (vector<double> vector, int row_no);

	static bool eq(double a, double b);
	static bool neq(double a, double b);
	static bool less(double a, double b);
	static bool le(double a, double b);
	static bool ge(double a, double b);
	static bool greater(double a, double b);

	static void matvectmult(double *vout, double **m, double *vin, int n);
	static void diagmatvectmult(double *vout, double *m, double *vin, int n);

	static bool fileExists (const std::string& name);
	static string waitOnFile(vector<string> files);
	static string waitOnFile(string file);

	/*template <class T>
	static T next(T x);

	template <class T, class Distance>
	static T next(T x, Distance n);

	template <class T>
	static T prior(T x);

	template <class T, class Distance>
	static T prior(T x, Distance n);*/
};

#endif /* UTILS_H_ */
