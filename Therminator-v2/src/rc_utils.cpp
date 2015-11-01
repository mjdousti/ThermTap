/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
*/
#include "headers/rc_utils.h"
#include <omp.h>
//#include <boost/numeric/odeint/external/openmp/openmp.hpp>
//#include <thrust/device_vector.h>
//#include <boost/numeric/odeint/external/thrust/thrust_algebra.hpp>
//#include <boost/numeric/odeint/external/thrust/thrust_operations.hpp>
//#include <boost/numeric/odeint/external/mkl/mkl_operations.hpp>

//double **RCutils::matrix;
//double *RCutils::invC_vector;
//double **RCutils::A_matrix;

double *RCutils::P_vector;
int RCutils::n;
int** RCutils::a_matrix_indices;
double** RCutils::a_matrix_values;

/* For Eigen */
//SparseMatrix<double> RCutils::a_matrix;
//VectorXd RCutils::p_vector;


#if USE_GPU==1
void RCutils::checkGPUStatus(culaStatus status){
    char buf[256];

    if(!status)
        return;

    culaGetErrorInfoString(status, culaGetErrorInfo(), buf, sizeof(buf));
    cout<<buf<<endl;

    culaShutdown();
    exit(-1);
}

void RCutils::gpuSolveSteady(double **matrix, int N, double* P_vector, double *T_vector){
    int NRHS = 1;

    culaStatus status;

    culaDouble* A = NULL;
    culaInt* IPIV = NULL;


    cout<<"--------------------"<<endl;
    cout<<" Calling GPU Solver"<<endl;
    cout<<"--------------------"<<endl;

    cout<<"Allocating Matrices..."<<endl;
    A = (culaDouble*)malloc(N*N*sizeof(culaDouble));
    IPIV = (culaInt*)malloc(N*sizeof(culaInt));
    if(!A || !IPIV){
        cerr<<"Cannot allocate contiguous memory for solving the linear set of equations using GPU."<<endl;
    	exit(-1);
    }

    cout<<"Initializing CULA..."<<endl;
    status = culaInitialize();
    RCutils::checkGPUStatus(status);

	for (int i = 0; i < N; i++){
	    memmove(&A[i*N], matrix[i], sizeof(double) * N);
	}

    //Making a copy of P_vector into T_vector; This is required per specification
    memcpy(T_vector, P_vector, N*sizeof(culaDouble));

    //memset(IPIV, 0, N*sizeof(culaInt));

    cout<<"Calling the solver..."<<endl;
    //General Solver
//	status = culaDgesv(N, NRHS, A, N, IPIV, T_vector, N);
    //Assuming that A is a positive-definite matrix
    status = culaDposv('U', N, NRHS, A, N, T_vector, N);
    if(status == culaInsufficientComputeCapability){
        cout<<"No Double precision support available."<<endl;
        delete(A);
        delete(IPIV);
        culaShutdown();
        return;
    }
    RCutils::checkGPUStatus(status);


    cout<<"Shutting down CULA..."<<endl<<endl;
    culaShutdown();

    delete(A);
    delete(IPIV);
}
#else

/**
 * This solver uses Eigen library to solve the linear set of equations
 */
void RCutils::cpuSolveSteady(double **matrix, int N, double* P_vector, vector<double>& T_vector){
//	Eigen::setNbThreads(8);
//	MatrixXd A = MatrixXd(N,N);

	SparseMatrix<double> A(N,N);
	vector<Triplet<double> > triplet;

	VectorXd b = VectorXd(N);
	//SparseVector<double> b(N);
	for (int i = 0; i < N; i++){
		for(int j=0; j<N; j++){
			//A(i,j)=matrix[i][j];
			if (matrix[i][j]!=0)
				triplet.push_back(Triplet<double> (i,j,matrix[i][j]));
		}
		b(i)=P_vector[i];
	}
	A.setFromTriplets(triplet.begin(), triplet.end());
	
	cout<<"Calling the solver..."<<endl;
	SimplicialCholesky<SparseMatrix<double> > chol(A);

	b = chol.solve(b);

	//b=A.llt().solve(b);
//	VectorXd x =  b;

	for (int i = 0; i < N; i++){
		T_vector[i]=b(i);
	}
}


void RCutils::thermEq(const state_type &T_vector, state_type &dTdt, double t){
// Code written for Eigen
//   const double *PtState = &T_vector[0];
//   double *PtChange = &dTdt[0];
//
//   typedef Map<const VectorXd> MapTypeConst; // a read-only map
//   typedef Map<VectorXd> MapType;
//
//
//   MapTypeConst States(PtState, n);
//   MapType Changes(PtChange, n);
//
//   Changes = p_vector - (a_matrix * States);


	#pragma omp parallel for schedule(auto) num_threads(4)
//	#pragma omp parallel for schedule(runtime)
	for(int i=0; i<n; i++){
		dTdt[i]=(P_vector[i]);

		for(int j=0; j<n; j++){
			if (a_matrix_indices[i][j]!=-1){
				dTdt[i]-= a_matrix_values[i][j] * T_vector[a_matrix_indices[i][j]];
			}else
				break;
		}

	}
	/*
	 * GPU:
	 *  	typedef thrust::device_vector<double> state_type;
	 * 		runge_kutta4< state_type , double , state_type , double , thrust_algebra ,	thrust_operations > rk4;
	 *
	 */




	//		for(int j=0; j<n; j++){
	//			if (A_matrix[i][j]!=0){
	//				dTdt[i]-=(A_matrix[i][j]*T_vector[j]);
	//			}
	//		}


}

void RCutils::writeTemp( const state_type &T_vector , const double t ){
	cout<<t<<":\t"<<T_vector[0]-273.15<<" C"<<"\n";
}


//template<std::size_t element_no>
void RCutils::cpuSolveTransient(int n, double ** A_matrix, int** a_matrix_indices, double** a_matrix_values,
		double *invC_vector, double* P_vector, vector<double> &T_vector, double time_start, double time_end){

	RCutils::a_matrix_indices=a_matrix_indices;
	RCutils::a_matrix_values=a_matrix_values;
	RCutils::n = n;

	/* Calculating P' = C^-1 * P  */
	RCutils::P_vector = Utils::vectorAlloc(n);
	Utils::diagmatvectmult(RCutils::P_vector, invC_vector, &P_vector[0], n);


	cout<<T_vector[0]-273.15<<" C"<<endl;


	// For Eigen
//	a_matrix=SparseMatrix<double>(n,n);
//	p_vector = VectorXd(n);
//
//	vector<Triplet<double> > triplet;
//	for (int i = 0; i < n; i++){
//		for(int j=0; j<n; j++){
//			if (A_matrix[i][j]!=0){
//				triplet.push_back(Triplet<double> (i, j, A_matrix[i][j]));
//			}
//		}
//		p_vector(i)=RCutils::P_vector[i];
//	}
//	a_matrix.setFromTriplets(triplet.begin(), triplet.end());



	//integrate( system , x0 , t0 , t1 , dt )
	//http://headmyshoulder.github.io/odeint-v2/doc/boost_numeric_odeint/odeint_in_detail/integrate_functions.html

	/* use the scratch pad vector to find (inv_A)*POWER */
	//vdMul(n, &P_vector[0], invC_vector, &P_vector[0]);

	double dt = 0.00001;

//	runge_kutta_cash_karp54 is better than runge_kutta_dopri5
	typedef  runge_kutta_cash_karp54 < state_type > error_stepper_type;
	typedef controlled_runge_kutta< error_stepper_type > controlled_stepper_type;
	controlled_stepper_type controlled_stepper;

//    adams_bashforth_moulton< 2 , state_type, double, state_type, double, openmp_range_algebra > stepper_adams;

//    int chunk_size = T_vector.size()/omp_get_max_threads();
//    omp_set_schedule( omp_sched_static , chunk_size );

	struct timespec start, end;

	cout<<"Calling the solver..."<<endl;
	clock_gettime(CLOCK_MONOTONIC, &start);

    integrate_adaptive( make_controlled( 5E-1 , 1E-1 , error_stepper_type() ) , thermEq , T_vector , time_start, time_end , dt);


//	integrate_adaptive(stepper_adams , thermEq , T_vector , time_start, time_end , dt);

//	integrate_adaptive( make_controlled<error_stepper_type>( 1E-3 , 1E-3) , thermEq , T_vector , time_start, time_end , dt);
//  integrate_adaptive(stepper_mew , thermEq , T_vector , time_start, time_end , dt);
//	integrate_adaptive(controlled_stepper, thermEq , T_vector , time_start, time_end , dt);

	clock_gettime(CLOCK_MONOTONIC, &end);
	double elapsed = (end.tv_sec - start.tv_sec);
	elapsed += (end.tv_nsec - start.tv_nsec) / 1e9;
	cout<<endl<<"ODE runtime: "<<elapsed<<" s"<<endl;

	cout<<T_vector[0]-273.15<<" C"<<endl;

	Utils::vectorDealloc(RCutils::P_vector);

//	Baseline: HotSpot code
//	double *power = Utils::vectorAlloc(n);
//	/* use the scratch pad vector to find (inv_A)*POWER */
//    diagmatvectmult(power, invC_vector, &P_vector[0], n);
//
//    /* Obtain temp at time (t+time_elapsed).
//     * Instead of getting the temperature at t+time_elapsed directly, we do it
//     * in multiple steps with the correct step size at each time
//     * provided by rk4
//     */
//	int i=0;
//	double h, time_elapsed = 0.1;
//
//	for (double t = 0, new_h = 1e-7 ; t < time_elapsed && new_h >= 1e-7*1.0e-6; t+=h) {
//        h = new_h;
//
//        /* the slope function callback is typecast accordingly */
//        new_h = rk4(A_matrix, &T_vector[0], &P_vector[0], n, &h, &T_vector[0], (slope_fn_ptr) slope_fn_block);
//        new_h = std::min(new_h, time_elapsed-t-h);
//    	cout << t << ":\t" << T_vector[0]-273.15<<" C"<<endl;
//        i++;
//    }
}


#endif
double RCutils::calcThermalConductivity(double k, double thickness, double area){
	if (thickness==0){
		cerr<<"The thickness of an element cannot be zero."<<endl;
		exit(-1);
	}
	return k * area / thickness;
}


double RCutils::calcSubComponentCapacitance(SubComponent *sc){
	double volume = sc->getLength() * sc->getWidth() * sc->getHeight();

	if (volume==0)
		cout<<sc->getName() << " has nil volume."<<endl;

	else if (sc->getComponent()->getMaterial()->getSpecificHeat()==0)
		cout<<sc->getName() << " has nil specific heat."<<endl;

	else if (sc->getComponent()->getMaterial()->getDensity()==0)
		cout<<sc->getName() << " has nil density."<<endl;


	return C_FACTOR * sc->getComponent()->getMaterial()->getSpecificHeat() *
			sc->getComponent()->getMaterial()->getDensity() * volume;
}


double RCutils::calcAmbientResistance(double h, double area){
	return 1/(h * area);
}

double RCutils::overallParallelConductivity(double k1, double k2){
	return (k1 * k2)/( k1 + k2 );
}


bool RCutils::touchesAirInXDir(SubComponent *sc, Device* device){
	if (Utils::eq(sc->getX(), device->getX()) ||
			Utils::eq(sc->getX() +  sc->getLength(), device->getX() +  device->getLength()))
		return true;
	else
		return false;
}

bool RCutils::touchesAirInYDir(SubComponent *sc, Device* device){
	if (Utils::eq(sc->getY(), device->getY()) ||
			Utils::eq(sc->getY() +  sc->getWidth(), device->getY() +  device->getWidth())){
		return true;
	}else{
		return false;
	}
}

bool RCutils::touchesAirFromTopBot(SubComponent *sc, Device* device){
	if (Utils::eq(sc->getZ() , device->getZ()) ||
			Utils::eq(sc->getZ() + sc->getHeight() , device->getZ() +  device->getHeight()))
		return true;
	else
		return false;
}

double RCutils::calcConductanceToAmbient(SubComponent *sc, Device* device){
	double commonArea;
	double t1=0;
	double k;
	double K=0;
	double Kx=0, Ky=0, Kz=0, Rx, Ry, Rz;
//	double h = 10 * 1.15; //	W/m^2/K, from <http://www.engineeringtoolbox.com/convective-heat-transfer-d_430.html>
	double h = 10 * 1.15;
	if (touchesAirFromTopBot(sc, device)){	//Touches air from top or bottom
		t1 = sc->getHeight() / 2;
		commonArea = sc->getLength() * sc->getWidth();
//		cout<<p1->getName() << " and "<<" have XY common area to ambient: "<<commonArea<<endl;

		k = sc->getComponent()->getMaterial()->getNormalConductivity();

		Kz= RCutils::calcThermalConductivity(k, t1, commonArea);
//		cout<<RCutils::calcThermalConductivity(k1, t1, commonArea)<<endl;
		Rz = RCutils::calcAmbientResistance(h, commonArea);
		Kz = RCutils::overallParallelConductivity(Kz, 1/Rz);
	}
	if (touchesAirInXDir(sc, device)){	//Touches air from the X side
		t1 = sc->getLength() / 2;
		commonArea = sc->getWidth() * sc->getHeight();
//		cout<<p1->getName() << " and "<<" have YZ common area to ambient: "<<commonArea<<endl;

		//Setting the k1 value to the proper value if the planar conductivity differs from the normal conductivity
		if (sc->getComponent()->getMaterial()->hasPlanarConductivity())
			k = sc->getComponent()->getMaterial()->getPlanarConductivity();
		else
			k = sc->getComponent()->getMaterial()->getNormalConductivity();


		Kx = RCutils::calcThermalConductivity(k, t1, commonArea);
		Rx = RCutils::calcAmbientResistance(h, commonArea);
		Kx = RCutils::overallParallelConductivity(Kx, 1/Rx);
	}
	if (touchesAirInYDir(sc , device)){		//Touches air from the Y side

		t1 = sc->getWidth() / 2;
		commonArea = sc->getLength() * sc->getHeight();
//		cout<<p1->getName() << " and "<<" have XZ common area to ambient: "<<commonArea<<endl;

		//Setting the k1 value to the proper value if the planar conductivity differs from the normal conductivity
		if (sc->getComponent()->getMaterial()->hasPlanarConductivity())
			k = sc->getComponent()->getMaterial()->getPlanarConductivity();
		else
			k = sc->getComponent()->getMaterial()->getNormalConductivity();

		Ky = RCutils::calcThermalConductivity(k, t1, commonArea);
		Ry = RCutils::calcAmbientResistance(h, commonArea);
		Ky = RCutils::overallParallelConductivity(Ky, 1/Ry);
	}
	return K= Kx + Ky + Kz;
}

double RCutils::calcCommonConductance(SubComponent *sc1, SubComponent *sc2){
	double commonArea;
	double t1=0, t2=0;
	double k1, k2;

	// common area in the Y & Z planes
	double commonX, commonY, commonZ;

	commonX= min(sc1->getX()+sc1->getLength() , sc2->getX()+sc2->getLength()) - max(sc1->getX() , sc2->getX());
	commonY= min(sc1->getY()+sc1->getWidth() , sc2->getY()+sc2->getWidth()) - max(sc1->getY() , sc2->getY());
	commonZ= min(sc1->getZ()+sc1->getHeight() , sc2->getZ()+sc2->getHeight()) - max(sc1->getZ() , sc2->getZ());


	if ((Utils::eq(sc1->getX() + sc1->getLength() , sc2->getX()) ||
		Utils::eq(sc2->getX() + sc2->getLength() , sc1->getX())) && commonZ>0 && commonY>0){
		commonArea = commonY * commonZ;
		 t1 = sc1->getLength() / 2;
		 t2 = sc2->getLength() / 2;

		 //using planar thermal conductivity if the material has different value for it
		 if (sc1->getComponent()->getMaterial()->hasPlanarConductivity()){
			 k1 = sc1->getComponent()->getMaterial()->getPlanarConductivity();
		 }else{
			 k1 = sc1->getComponent()->getMaterial()->getNormalConductivity();
		 }
		 if (sc2->getComponent()->getMaterial()->hasPlanarConductivity()){
			 k2 = sc2->getComponent()->getMaterial()->getPlanarConductivity();
		 }else{
			 k2 = sc2->getComponent()->getMaterial()->getNormalConductivity();
		 }
//		 cout<<"Common area (YZ) btn "<<p1->getName()<<" and "<<p2->getName()<<" is "<<commonArea<<endl;
	}else if ((Utils::eq(sc1->getY() + sc1->getWidth() , sc2->getY()) ||
			Utils::eq(sc2->getY() + sc2->getWidth() , sc1->getY())) && commonX>0 && commonZ>0){
		commonArea = commonX * commonZ;
		 t1 = sc1->getWidth() / 2;
		 t2 = sc2->getWidth() / 2;

		 //using planar thermal conductivity if the material has different value for it
		 if (sc1->getComponent()->getMaterial()->hasPlanarConductivity()){
			 k1 = sc1->getComponent()->getMaterial()->getPlanarConductivity();
		 }else{
			 k1 = sc1->getComponent()->getMaterial()->getNormalConductivity();
		 }
		 if (sc2->getComponent()->getMaterial()->hasPlanarConductivity()){
			 k2 = sc2->getComponent()->getMaterial()->getPlanarConductivity();
		 }else{
			 k2 = sc2->getComponent()->getMaterial()->getNormalConductivity();
		 }
//		 cout<<"Common area (XZ) btn "<<p1->getName()<<" and "<<p2->getName()<<" is "<<commonArea<<endl;
	}else if ((Utils::eq(sc1->getZ() + sc1->getHeight() , sc2->getZ()) ||
			Utils::eq(sc2->getZ() + sc2->getHeight(), sc1->getZ())) && commonX>0 && commonY>0){
		commonArea = commonX * commonY;
		t1 = sc1->getHeight() / 2;
		t2 = sc2->getHeight() / 2;

		//using normal conductivity since it is in the vertical direction
		 k1 = sc1->getComponent()->getMaterial()->getNormalConductivity();
		 k2 = sc2->getComponent()->getMaterial()->getNormalConductivity();

//		 cout<<"Common area (XY) btn "<<p1->getName()<<" and "<<p2->getName()<<" is "<<commonArea<<endl;
	}else{
		commonArea = 0;
		return 0;
	}
	//cout<<p1->getName() << " and "<<p2->getName() <<" have common area: "<<commonArea<<endl;

	double K1 = RCutils::calcThermalConductivity(k1, t1, commonArea);
	double K2 = RCutils::calcThermalConductivity(k2, t2, commonArea);

	return RCutils::overallParallelConductivity(K1,K2);
}









/* compute the slope vector dy for the transient equation
 * dy + cy = p. useful in the transient solver
 */
void slope_fn_block(double *y, double *p, double *dy, double **c, int n)
{
    /* for our equation, dy = p - cy */
    int i;
    double *t = Utils::vectorAlloc(n);
    Utils::matvectmult(t, c, y, n);
    for (i = 0; i < n; i++)
        dy[i] = p[i]-t[i];
    Utils::vectorDealloc(t);
}


/* slope function pointer - used as a call back by the transient solver */
typedef void (*slope_fn_ptr)(void *y, void *p, void *dy, double **c, int n);

/* core of the 4th order Runge-Kutta method, where the Euler step
 * (y(n+1) = y(n) + h * k1 where k1 = dydx(n)) is provided as an input.
 * to evaluate dydx at different points, a call back function f (slope
 * function) is also passed as a parameter. Given values for y, and k1,
 * this function advances the solution over an interval h, and returns
 * the solution in yout. For details, see the discussion in "Numerical
 * Recipes in C", Chapter 16, from
 * http://www.nrbook.com/a/bookcpdf/c16-1.pdf
 */
void rk4_core(double **c, double *y, double *k1, void *p, int n, double h, double *yout, slope_fn_ptr f)
{
	int i;
	double *t, *k2, *k3, *k4;
	k2 = Utils::vectorAlloc(n);
	k3 = Utils::vectorAlloc(n);
	k4 = Utils::vectorAlloc(n);
	t = Utils::vectorAlloc(n);

	/* k2 is the slope at the trial midpoint (t) found using
	 * slope k1 (which is at the starting point).
	 */
	/* t = y + h/2 * k1 (t = y; t += h/2 * k1) */
	for(i=0; i < n; i++)
		t[i] = y[i] + h/2.0 * k1[i];
	/* k2 = slope at t */
	(*f)(t, p, k2, c, n);

	/* k3 is the slope at the trial midpoint (t) found using
	 * slope k2 found above.
	 */
	/* t =  y + h/2 * k2 (t = y; t += h/2 * k2) */
	for(i=0; i < n; i++)
		t[i] = y[i] + h/2.0 * k2[i];
	/* k3 = slope at t */
	(*f)(t, p, k3, c, n);

	/* k4 is the slope at trial endpoint (t) found using
	 * slope k3 found above.
	 */
	/* t =  y + h * k3 (t = y; t += h * k3) */
	for(i=0; i < n; i++)
		t[i] = y[i] + h * k3[i];
	/* k4 = slope at t */
	(*f)(t, p, k4, c, n);

	/* yout = y + h*(k1/6 + k2/3 + k3/3 + k4/6)	*/

	for (i =0; i < n; i++)
		yout[i] = y[i] + h * (k1[i] + 2*k2[i] + 2*k3[i] + k4[i])/6.0;

	Utils::vectorDealloc(k2);
	Utils::vectorDealloc(k3);
	Utils::vectorDealloc(k4);
	Utils::vectorDealloc(t);
}

void copy_dvector (double *dst, double *src, int n){
    memmove(dst, src, sizeof(double) * n);
}

#define RK4_SAFETY      0.95
#define RK4_MAXUP       5.0
#define RK4_MAXDOWN     10.0
#define RK4_PRECISION   0.01
double rk4(double **c, double *y, void *p, int n, double *h, double *yout, slope_fn_ptr f)
{
    int i;
    double *k1, *t1, *t2, *ytemp, max, new_h = (*h);

    k1 = Utils::vectorAlloc(n);
    t1 = Utils::vectorAlloc(n);
    t2 = Utils::vectorAlloc(n);
    ytemp = Utils::vectorAlloc(n);

    /* evaluate the slope k1 at the beginning */
    (*f)(y, p, k1, c, n);

    /* try until accuracy is achieved   */
    do {
        (*h) = new_h;

        /* try RK4 once with normal step size   */
        rk4_core(c, y, k1, p, n, (*h), ytemp, f);

        /* repeat it with two half-steps    */
        rk4_core(c, y, k1, p, n, (*h)/2.0, t1, f);

        /* y after 1st half-step is in t1. re-evaluate k1 for this  */
        (*f)(t1, p, k1, c, n);

        /* get output of the second half-step in t2 */
        rk4_core(c, t1, k1, p, n, (*h)/2.0, t2, f);

        /* find the max diff between these two results:
         * use t1 to store the diff
         */
		for(i=0; i < n; i++)
			t1[i] = fabs(ytemp[i] - t2[i]);
		max = t1[0];
		for(i=1; i < n; i++)
			if (max < t1[i])
				max = t1[i];

	       /*
	         * compute the correct step size: see equation
	         * 16.2.10  in chapter 16 of "Numerical Recipes
	         * in C"
	         */
	        /* accuracy OK. increase step size  */
	        if (max <= RK4_PRECISION) {
	            new_h = RK4_SAFETY * (*h) * pow(fabs(RK4_PRECISION/max), 0.2);
	            if (new_h > RK4_MAXUP * (*h))
	                new_h = RK4_MAXUP * (*h);
	        /* inaccuracy error. decrease step size and compute again */
	        } else {
	            new_h = RK4_SAFETY * (*h) * pow(fabs(RK4_PRECISION/max), 0.25);
	            if (new_h < (*h) / RK4_MAXDOWN)
	                new_h = (*h) / RK4_MAXDOWN;
	        }

	    } while (new_h < (*h));

	    /* commit ytemp to yout */
	    copy_dvector(yout, ytemp, n);

	    /* clean up */
	    Utils::vectorDealloc(k1);
	    Utils::vectorDealloc(t1);
	    Utils::vectorDealloc(t2);
	    Utils::vectorDealloc(ytemp);


	    /* return the step-size */
	    return new_h;
}



/*
 * LUP decomposition from the pseudocode given in the CLRS book
 * ('Introduction to Algorithms'). The matrix 'a' is
 * transformed into an in-place lower/upper triangular matrix
 * and the vector'p' carries the permutation vector such that
 * Pa = lu, where 'P' is the matrix form of 'p'.
 */

void RCutils::lupdcmp(double**a, int n, int *p){
	int i, j, k, pivot=0;
	double max = 0;

	/* start with identity permutation	*/
	for (i=0; i < n; i++)
		p[i] = i;

	for (k=0; k < n; k++)	 {
		max = 0;
		for (i = k; i < n; i++)	{
			if (fabs(a[i][k]) > max) {
				max = fabs(a[i][k]);
				pivot = i;
			}
		}
		if (Utils::eq (max, 0)){
			cerr<<"Singular matrix in lupdcmp."<<endl;
			exit (-1);
		}

		/* bring pivot element to position	*/
		swap(p[k], p[pivot]);
		for (i=0; i < n; i++){
			swap(a[k][i], a[pivot][i]);
		}

		for (i=k+1; i < n; i++) {
			a[i][k] /= a[k][k];
			for (j=k+1; j < n; j++)
				a[i][j] -= a[i][k] * a[k][j];
		}
	}
}

/*
 * the matrix a is an in-place lower/upper triangular matrix
 * the following macros split them into their constituents
 */

#define LOWER(a, i, j)		((i > j) ? a[i][j] : 0)
#define UPPER(a, i, j)		((i <= j) ? a[i][j] : 0)


/*
 * LU forward and backward substitution from the pseudocode given
 * in the CLRS book ('Introduction to Algorithms'). It solves ax = b
 * where, 'a' is an in-place lower/upper triangular matrix. The vector
 * 'x' carries the solution vector. 'p' is the permutation vector.
 */

void RCutils::lusolve(double **a, int n, int *p, double *b, double *x){
	int i, j;
	double *y = new double[n];
	double sum;

	/* forward substitution	- solves ly = pb	*/
	for (i=0; i < n; i++) {
		for (j=0, sum=0; j < i; j++)
			sum += y[j] * LOWER(a, i, j);
		y[i] = b[p[i]] - sum;
	}

	/* backward substitution - solves ux = y	*/
	for (i=n-1; i >= 0; i--) {
		for (j=i+1, sum=0; j < n; j++)
			sum += x[j] * UPPER(a, i, j);
		x[i] = (y[i] - sum) / UPPER(a, i, i);
	}

	delete[] y;
}
