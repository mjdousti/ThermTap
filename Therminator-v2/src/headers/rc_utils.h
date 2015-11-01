/*
 * 
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab, 
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/

#ifndef RCUTILS_H_
#define RCUTILS_H_
#include "utils.h"
#include "physical_entity.h"
#include "device.h"

#include <iostream>


#define EIGEN_USE_MKL_ALL
/* 
 * A quick and dirty fix to support MKL 11.2 
 * http://eigen.tuxfamily.org/bz/show_bug.cgi?id=874i
 *
 */
#ifndef MKL_BLAS
	#define MKL_BLAS MKL_DOMAIN_BLAS
#endif

#include <Eigen/Dense>
#include <Eigen/Sparse>
#include <Eigen/Core>
#include <boost/numeric/odeint/stepper/runge_kutta_dopri5.hpp>
using namespace Eigen;
using namespace boost::numeric::odeint;


#if USE_GPU==1
#include <cula_lapack.h>
#endif


/* model specific constants */
/* changed from 1/2 to 1/3 due to the difference from traditional Elmore Delay scenario */
//TODO: Check C_FACTOR
#define C_FACTOR    0.5       /* fitting factor to match floworks (due to lumping)    */

typedef vector< double > state_type;
//typedef VectorXd state_type;



class RCutils {
public:
//	void transientRelation(const boost::array<double, 2> &x , boost::array<double, 2> &dxdt , double t );
//	template<std::size_t element_no>
	static void thermEq(const state_type &T_vector, state_type &dTdt, double t);
	static void writeTemp( const state_type &x , const double t );
	static void cpuSolveTransient(int n, double ** A_matrix, int** a_matrix_indices, double** a_matrix_values,
			double *invC_vector, double* P_vector, vector<double> &T_vector, double time_start, double time_end);


#if USE_GPU==1
	static void checkGPUStatus(culaStatus status);
	static void gpuSolveSteady(double **LU, int elements_no, double* P_vector, double *T_vector);
#else
	static void lupdcmp(double**a, int n, int *p);
	static void lusolve(double **a, int n, int *p, double *b, double *x);
	static void cpuSolveSteady(double **matrix, int N, double* P_vector, vector<double>& T_vector);
#endif
	static double calcConductanceToAmbient(SubComponent *sc, Device* d);
	static double calcCommonConductance(SubComponent *sc1, SubComponent *sc2);
	static double overallParallelConductivity(double k1, double k2);
	static double calcAmbientResistance(double h, double area);
	static double calcThermalConductivity(double k, double thickness, double area);

	static double calcSubComponentCapacitance(SubComponent *sc);

	static bool touchesAirInXDir(SubComponent *sc, Device* device);
	static bool touchesAirInYDir(SubComponent *sc, Device* device);
	static bool touchesAirFromTopBot(SubComponent *sc, Device* device);

private:

//	static double **matrix;
//	static double *invC_vector;
//	static double **A_matrix;
	static double *P_vector;
	static int n;
	static int** a_matrix_indices;
	static double** a_matrix_values;

	/* For Eigen */
	static SparseMatrix<double> a_matrix;
	static VectorXd p_vector;

};

#endif /* RCUTILS_H_ */
