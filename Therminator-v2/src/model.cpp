/*
 *
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab,
 * University of Southern California. All rights reserved.
 *
 * Please refer to the LICENSE file for terms of use.
 *
 */

#include "headers/model.h"

Model::Model(Device *d, bool isTransient){
	pwr_consumers_cnt = 0;
	transient = isTransient;
	//P & G matrices should be made later
	T_vector_made = P_vector_made = G_matrix_made = C_vector_made = false;

	device=d;
	elements_no=device->getComponentCount();
	cout<<"Component count: "<<elements_no<<endl;

	G_matrix= Utils::matrixAlloc(elements_no, elements_no);
	amb_vector = Utils::vectorAlloc(elements_no);

	if (transient){
		A_matrix = Utils::matrixAlloc(elements_no, elements_no);
		invC_vector = Utils::vectorAlloc(elements_no);

		a_matrix_indices = Utils::matrixIntAlloc(elements_no, elements_no);
		a_matrix_values = Utils::matrixAlloc(elements_no, elements_no);
	}
}


void Model::allocateNewPwrTempVector(){
	//Allocating space for the power vector
	P_vector.push_back(new double [elements_no]);

	//Allocating space for the temperature vector
	vector<double> t_vector;
	t_vector.resize(elements_no, 0);
	T_vector.push_back(t_vector);
	
	int index = P_vector.size()-1;
	for (int j = 0; j < elements_no; j++) {
		P_vector[index][j]=amb_vector[j];
		T_vector[index][j]=0.0;
	}
}

void Model::initPwrTempVector(){
	//Initializing the vectors to zero
	for (unsigned i=0; i<P_vector.size(); i++){
		for (int j = 0; j < elements_no; j++) {
			P_vector[i][j]=amb_vector[j];
		}
	}
}

bool Model::isComment(string s){
	//skipping from comment
	size_t found;
	found = s.find_first_not_of(" \t");

	if (found==string::npos || s[found]=='#'){	//it either consists of whitespace or comment
		return true;
	}else{
		return false;
	}
}

void Model::preparePVector(){
	string temp;
	list<string> tokenList;
	vector<SubComponent *> subComponents = device->getSubComponents();
	SubComponent* sc;
	int i=0;
	pwr_consumers_cnt = 0;

	//Storing the device subcomponents order in a map, i.e., powerMappingDeviceOrder in order to access them while filling the P vector
	for(vector<SubComponent *>::iterator it1 = subComponents.begin(); it1 != subComponents.end(); it1++){
		sc=(*it1);
		//avoid using (i,j) concatenated with the names in the high-res components
		if (sc->isPrimary()){
			powerMappingDeviceOrder.insert(pair<string, int> (sc->getComponent()->getName(), i));
		}else{
			powerMappingDeviceOrder.insert(pair<string, int> (sc->getName(), i));
		}
		if (sc->getComponent()->isPowerGen() && (sc->getComponent()->hasFloorPlan() || sc->isPrimary())){
			pwr_consumers_cnt++;
		}
		i++;
	}
	//Opening the power trace file for reading
	string powerTraceFileAddr = device->getPowerTraceFile();

	powerTraceFile.open(powerTraceFileAddr.c_str(), ifstream::in);

	if (!powerTraceFile.is_open()){
		cerr<<"Could not open "<<powerTraceFileAddr<<" for parsing as the power trace."<<endl;
		exit(-1);
	}


	do{
		getline(powerTraceFile, temp);
		algorithm::trim(temp);
	}while (isComment(temp) && powerTraceFile.eof()==false);

	if (powerTraceFile.eof()){
		cerr<<"The file does not contain valid power trace."<<endl;
		exit (-1);
	}

	split(tokenList, temp, is_any_of("\t "), token_compress_on);
	i=0;
	BOOST_FOREACH(string token, tokenList){
		powerMappingTraceOrder.insert(pair<int, string> (i, token));
		i++;
	}
	//If the number of titles and the power consumer subcomponents do not match, exit.
	if (pwr_consumers_cnt != i){
		cerr<<"The number of (sub)component power trace in "<<powerTraceFileAddr<< " do not match with the XML description."
				<<" The expected sub(component) count was "<<pwr_consumers_cnt<<" but "<<i<<" subcomponents are provided."<<endl;
		exit (-1);
	}
}


int Model::read_power(){
	string temp;
	list<string> tokenList;
	int i, 	subComponentIndex;
	int count=0;

	P_vector_made = true;
	T_vector_made = false;

	initPwrTempVector();

/*
// This is only for testing
 	if ((int)P_vector.size() >0){
//		cout<<"RETURN"<<endl;
		return (int)P_vector.size();
	}else{
//		cout<<"Going THROUGH!"<<endl;
	}
*/
	preparePVector();

	do{
		do{
			if (powerTraceFile.eof()==false){
				getline(powerTraceFile, temp);

				algorithm::trim(temp);
			}else{
				powerTraceFile.close();
				return count;
			}
		}while (isComment(temp));

		//A P_vector is required to build proper right_hand side

		if ((int)P_vector.size()<=count){
			allocateNewPwrTempVector();
		}

		split(tokenList, temp, is_any_of("\t "), token_compress_on);
		i=0;
		//Adding the power consumption of subcomponents to the P vector
		BOOST_FOREACH(string token, tokenList){
			if (powerMappingDeviceOrder.find(powerMappingTraceOrder[i])==powerMappingDeviceOrder.end()){
				cerr<<"Component "<<powerMappingTraceOrder[i]<<" is listed in the trace file but not found in the design file."<<endl;
				powerTraceFile.close();
				exit (-1);
			}
			subComponentIndex = powerMappingDeviceOrder[powerMappingTraceOrder[i]];

			//If the subcomponent has a resolution, assume the uniform heat distribution for that component
			if (device->getSubComponent(subComponentIndex)->isPrimary()){
				for (int j = subComponentIndex;
						j < subComponentIndex + device->getSubComponent(subComponentIndex)->getComponent()->getSubComponentCount(); j++) {
					P_vector[count][j] += atof(token.c_str()) / device->getSubComponent(subComponentIndex)->getComponent()->getSubComponentCount();
				}
			}else{
				P_vector[count][subComponentIndex] = atof(token.c_str());
			}
			i++;
		}

		//If the power values and the power consumer subcomponents do not match, exit.
		if (pwr_consumers_cnt != i){
			cerr<<"The number of (sub)component power trace in "<<powerTraceFileAddr<< " do not match with the XML description."
					<<" The expected sub(component) count was "<<pwr_consumers_cnt<<" but "<<i<<" subcomponents are provided."<<endl;
			exit (-1);
		}
		count++;
	}while(powerTraceFile.eof()==false);

	powerTraceFile.close();
	return count;
}

void Model::makeCapacitanceModel(){
	int i=0;
	SubComponent *sc;
	vector<SubComponent *> subComponents = device->getSubComponents();
	int n = subComponents.size();

	if (!G_matrix_made){
		cerr<<"G matrix is not initialized."<<endl;
		exit(-1);
	}

	double cap, area;
	//TODO: Find a proper value for c_convec_per_area
	double c_convec_per_area = 39e3;
	for(vector<SubComponent *>::iterator it1 = subComponents.begin(); it1 != subComponents.end(); it1++) {
		sc = (*it1);

		cap = RCutils::calcSubComponentCapacitance(sc);

		if(RCutils::touchesAirFromTopBot(sc, device)){
			area = sc->getLength() * sc->getWidth();
			cap += C_FACTOR * c_convec_per_area * area;
		}
		if(RCutils::touchesAirInXDir(sc, device)){
			area = sc->getWidth() * sc->getHeight();
			cap += C_FACTOR * c_convec_per_area * area;
		}
		if(RCutils::touchesAirInYDir(sc, device)){
			area = sc->getLength() * sc->getHeight();
			cap += C_FACTOR * c_convec_per_area * area;
		}

		invC_vector[i] = 1 / cap;
		for(int j=0;j<n; j++){
			A_matrix[i][j] = G_matrix[i][j] * invC_vector[i];
		}
		i++;
	}

	if (transient){
		int k;
		#pragma omp parallel for private(k) schedule(runtime)
		for (int i=0; i<elements_no; i++){
			k=0;
			for (int j=0; j<elements_no; j++){
				if (A_matrix[i][j]!=0){
					a_matrix_indices[i][k]=j;
					a_matrix_values[i][k]=A_matrix[i][j];
					k++;
				}
			}
			if (k<elements_no){
				a_matrix_indices[i][k]=-1;
			}
		}
	}




	C_vector_made = true;
}

void Model::makeResistanceModel(){
	int i=0, j=0;

	SubComponent *sc1, *sc2;
	vector<SubComponent *> subComponents = device->getSubComponents();

	for(vector<SubComponent *>::iterator it1 = subComponents.begin(); it1 != subComponents.end(); it1++) {
		sc1 = (*it1);
		vector<SubComponent *>::iterator it1_next=it1;
		it1_next++;
		j=i;
		for(vector<SubComponent *>::iterator it2 =  it1_next; it2 != subComponents.end(); it2++) {
			j++;
			sc2 = (*it2);
			double commonConductance=RCutils::calcCommonConductance(sc1, sc2);
			if (Utils::neq(commonConductance, 0)){
				G_matrix[i][j]=G_matrix[j][i] = -commonConductance;
			}
		}
		//Adding the conductances to air for items in the borders of the device
		double K1 = RCutils::calcConductanceToAmbient(sc1, device);
		if (Utils::neq(K1,0)){
			G_matrix[i][i]+= K1;

			amb_vector[i]=device->getTemperature() * K1;

		}
		else{
			amb_vector[i]=0;
		}
		i++;
	}

	//Calculating the diagonal elements
	for (int i = 0; i < elements_no; i++) {
		for (int j = 0; j < elements_no; j++) {
			if (i!=j)
				G_matrix[i][i]-= G_matrix[i][j];
		}
	}

	G_matrix_made=true;
}

void Model::solveSteadyState(){
	if (!P_vector_made){
		cerr<<"P vector is not initialized."<<endl;
		exit (-1);
	}else if (!G_matrix_made){
		cerr<<"G matrix is not initialized."<<endl;
		exit (-1);
	}
	for (unsigned i=0;i<T_vector.size();i++){
#if USE_GPU==1
		RCutils::gpuSolveSteady(G_matrix, elements_no, P_vector[i], T_vector[i]);
#else
		RCutils::cpuSolveSteady(G_matrix, elements_no, P_vector[i], T_vector[i]);
	}

	//	int *p = new int [elements_no];
	//	double** LU=Utils::matrixAlloc(elements_no, elements_no);
	//	Utils::matrixCopy(LU, G_matrix, elements_no, elements_no);
	//	RCutils::lupdcmp(LU, elements_no, p);
	//	RCutils::lusolve(LU, elements_no, p, P_vector, T_vector);
	//	Utils::matrixDealloc(LU, elements_no);
	//	delete[] p;
#endif
	T_vector_made =true;
}


void Model::solveTransientState(){
	if (!P_vector_made){
		cerr<<"P vector is not initialized."<<endl;
		exit (-1);
	}else if (!G_matrix_made){
		cerr<<"G matrix is not initialized."<<endl;
		exit (-1);
	}
	
	for (unsigned i=0;i<T_vector.size();i++){
		if (T_vector[i][0]==0){	//no initial temperature is assigned
			//solveSteadyState();
			cerr<<"No initial power trace is provided."<<endl;
			exit(-1);
		}else{
			RCutils::cpuSolveTransient(elements_no, G_matrix, a_matrix_indices, a_matrix_values, invC_vector, P_vector[i], T_vector[i], 0, 1);
		}
	}

	T_vector_made =true;
}

void Model::printComponentTemp(string file_output){
	ofstream temp_out;
	SubComponent *sc=NULL;
	if (!T_vector_made){
		cerr<<"The temperature is not calculated."<<endl;
		return;
	}
	double temp_avg=0;
	double temp_max = 0;
	int count=0, temp_width=0, blk_size=0, z_index=0;


	for (unsigned i=0;i<T_vector.size();i++){
		temp_out.open((file_output+"_"+to_string(i)).c_str(), ofstream::out);
		for (int j = 0; j < elements_no; j++) {
			sc = device->getSubComponent(j);
			if (sc->isPrimary()){
				if(j>0){
//					cout<<device->getSubComponent(j-1)->getComponent()->getName()<<":\t"<<
//							Utils::KtoC(temp_max)<<" C\t"<<Utils::KtoC(temp_avg/count)<<" C"<<endl;
				}
				//Do NOT put this line above the previous if statement
				count = 1;

				temp_width = device->getSubComponent(j)->getComponent()->getResolution().width;
				blk_size = (device->getSubComponent(j)->getComponent()->getResolution().length)*temp_width;
				z_index = device->getSubComponent(j)->getComponent()->getResolution().height;

				temp_out<<device->getSubComponent(j)->getComponent()->getName()<<":"<<endl;
				temp_out<<"z=" << z_index <<endl;

				temp_out<<fixed<<setprecision(1) <<Utils::KtoC(T_vector[i][j])<<"\t";
				temp_avg = T_vector[i][j];
				temp_max = T_vector[i][j];

			}else{
				count++;;
				temp_avg += T_vector[i][j];
				temp_max = max(T_vector[i][j], temp_max);

				temp_out<<fixed<<setprecision(1) <<Utils::KtoC(T_vector[i][j])<<"\t";
			}
			if (count%temp_width == 0){
				temp_out<<endl;
			}
			if (count%(blk_size)== 0 && z_index>1){
				z_index--;
				temp_out<<"z="<<z_index<<endl;
			}

		}
//		cout<<sc->getComponent()->getName()<<":\t"<<Utils::KtoC(temp_max)<<" C\t"<<Utils::KtoC(temp_avg/count)<<" C"<<endl;
		temp_out.close();
	}
}
void Model::printSubComponentTemp(){
	SubComponent *sc;
	if (!T_vector_made){
		cerr<<"The temperature is not calculated."<<endl;
		return;
	}
	for (unsigned i=0;i<T_vector.size();i++){
		for (int j = 0; j < elements_no; ++j) {
			sc = device->getSubComponent(j);
			cout<<sc->getName()<<":\t"<<Utils::KtoC(T_vector[i][j])<< " C"<<endl;
		}
	}
}
void Model::printTVector(){
	if (!T_vector_made){
		cerr<<"T vector is not initialized."<<endl;
		return;
	}
	for (unsigned i=0;i<T_vector.size();i++){
		cout<<"T["<<i<<"]=";
		Utils::dumpVector(T_vector[i], elements_no);
		cout<<endl;
	}
}

void Model::printGMatrix(){
	if (!G_matrix_made){
		cerr<<"G matrix is not initialized."<<endl;
		return;
	}
	cout<<"G=";
	Utils::dumpMatrix(G_matrix, elements_no, elements_no);
}
void Model::printCVector(){
	if (!C_vector_made){
		cerr<<"C vector is not initialized."<<endl;
		return;
	}
	cout<<"C=";
	Utils::dumpVector(invC_vector, elements_no);
}

void Model::printPVector(){
	if (!P_vector_made){
		cerr<<"P vector is not initialized."<<endl;
		return;
	}
	for (unsigned i=0;i<P_vector.size();i++){
		cout<<"P["<<i<<"]=";
		Utils::dumpVector(P_vector[i], elements_no);
		cout<<endl;
	}
}

Model::~Model(){
	delete(device);
	Utils::matrixDealloc(G_matrix, elements_no);

	for (unsigned i=0;i<P_vector.size();i++){
		delete[] P_vector[i];
	}
	delete[] amb_vector;

	//	delete[] T_vector;
	if (transient){
		Utils::matrixDealloc(A_matrix, elements_no);
		Utils::vectorDealloc(invC_vector);

		Utils::matrixDealloc(a_matrix_values, elements_no);
		Utils::matrixDealloc(a_matrix_indices, elements_no);
	}
}
