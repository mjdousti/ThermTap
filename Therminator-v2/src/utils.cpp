/*
 * 
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab, 
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
 */

#include "headers/utils.h"
#define NEGLIGIBLE_EPSILON 1e-10

double Utils::KtoC(double temp){
	return temp - 273.15;
}
bool Utils::eq(double a, double b)
{
	//return fabs(a - b) < numeric_limits<double>::epsilon();
	return fabs(a - b) < NEGLIGIBLE_EPSILON;
}

bool Utils::neq(double a, double b)
{
	return !eq(a,b);
}

bool Utils::less(double a, double b)
{
	return (b-a) > NEGLIGIBLE_EPSILON;
}

bool Utils::greater(double a, double b)
{
	return (a-b) > NEGLIGIBLE_EPSILON;
}

bool Utils::le(double a, double b)
{
	return ((a < b) || eq(a,b));
}

bool Utils::ge(double a, double b)
{
	return ((a > b) || eq(a,b));
}

bool Utils::isDouble(std::string const& s) {
	std::istringstream ss(s);
	double d;
	return (ss >> d) && (ss >> std::ws).eof();
}

void Utils::matrixCopy (double **dst, double **src, int row_no, int col_no){
	// There are excellent notes here:
	//	  http://stackoverflow.com/questions/2225850/c-c-how-to-copy-a-multidimensional-char-array-without-nested-loops
	for (int i = 0; i < row_no; i++) {
		memmove(dst[i], src[i], sizeof(double) * col_no);
	}
}

double **Utils::matrixAlloc (int row_no, int col_no){
	double **matrix = new double*[row_no];

	for (int i = 0; i < row_no; i++) {
		matrix[i]=new double [col_no];
		for (int j = 0; j < col_no; j++) {
			matrix[i][j]=0;
		}
	}
	return matrix;
}

int **Utils::matrixIntAlloc (int row_no, int col_no){
    int **matrix = new int*[row_no];

    for (int i = 0; i < row_no; i++) {
        matrix[i]=new int [col_no];
        for (int j = 0; j < col_no; j++) {
            matrix[i][j]=0;
        }
    }
    return matrix;
}

float **Utils::matrixFloatAlloc (int row_no, int col_no){
	float **matrix = new float*[row_no];

	for (int i = 0; i < row_no; i++) {
		matrix[i]=new float [col_no];
		for (int j = 0; j < col_no; j++) {
			matrix[i][j]=0;
		}
	}
	return matrix;
}


double *Utils::vectorAlloc (int items){
	double *vector = new double[items];

	for (int i = 0; i < items; i++) {
		vector[i]=0;
	}
	return vector;
}

void Utils::matrixDealloc (double **matrix, int row_no){
	for (int i = 0; i < row_no; i++) {
		delete[] matrix[i];
	}
	delete[] matrix;
}

void Utils::matrixDealloc (float **matrix, int row_no){
	for (int i = 0; i < row_no; i++) {
		delete[] matrix[i];
	}
	delete[] matrix;
}


void Utils::matrixDealloc (int **matrix, int row_no){
	for (int i = 0; i < row_no; i++) {
		delete[] matrix[i];
	}
	delete[] matrix;
}

void Utils::vectorDealloc (float *vector){
	delete[] vector;
}

void Utils::vectorDealloc (double *vector){
	delete[] vector;
}

void Utils::dumpMatrix (double **matrix, int row_no, int col_no){
	cout<<"[";
	for (int i = 0; i < row_no; i++) {
		for (int j = 0; j < col_no; j++) {
//			cout << matrix[i][j] <<" ";
			printf("%.10lf ", matrix[i][j]);
		}
		cout<<";"<<endl;
	}
		cout<<"];"<<endl;
}

void Utils::dumpVector (double *vector, int row_no){
	cout<<"[";
	for (int i = 0; i < row_no; i++) {
//		cout << vector[i] <<"; ";
		printf("%.10lf; ", vector[i]);
	}
	cout<<"];"<<endl;
}
void Utils::dumpVector (vector<double> vector, int row_no){
	cout<<"[";
	for (int i = 0; i < row_no; i++) {
		cout << vector[i] <<"; ";
	}
	cout<<"];"<<endl;
}

string Utils::waitOnFile(string file){
	vector<string> files;
	files.push_back(file);

	return waitOnFile(files);
}

string Utils::waitOnFile(vector<string> files){
	Inotify notify;

	InotifyWatch watch(".", IN_CLOSE_WRITE);
	notify.Add(watch);

	string fileWritten;

	do{
		notify.WaitForEvents();

		size_t count = notify.GetEventCount();
		while (count > 0) {
			InotifyEvent event;
			bool got_event = notify.GetEvent(&event);

			if (got_event) {
				string mask_str;
				event.DumpTypes(mask_str);
				BOOST_FOREACH(string file, files){
					if (event.GetName().compare(file)==0){
						fileWritten = file;
						break;
					}
				}
			}

			count--;
		}
	}while (fileWritten.empty());

	return fileWritten;
}

bool Utils::fileExists (const std::string& name){
    if (FILE *file = fopen(name.c_str(), "r")) {
        fclose(file);
        return true;
    } else {
        return false;
    }   
}

/* mult of an n x n matrix and an n x 1 column vector   */
void Utils::matvectmult(double *vout, double **m, double *vin, int n){
    int i, j;

    for (i = 0; i < n; i++) {
        vout[i] = 0;
        for (j = 0; j < n; j++)
            vout[i] += m[i][j] * vin[j];
    }
}

/* same as above but 'm' is a diagonal matrix stored as a 1-d array */
void Utils::diagmatvectmult(double *vout, double *m, double *vin, int n){
//	#pragma omp parallel for schedule(auto) num_threads(4)
    for (int i = 0; i < n; i++)
        vout[i] = m[i] * vin[i];
}

/*template <class T> T Utils::next(T x) {
	return ++x;
}

template <class T, class Distance>
T Utils::next(T x, Distance n){
    std::advance(x, n);
    return x;
}

template <class T>
T Utils::prior(T x) {
	return --x;
}

template <class T, class Distance>
T Utils::prior(T x, Distance n){
    std::advance(x, -n);
    return x;
}
 */
