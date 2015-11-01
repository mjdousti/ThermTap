/*
 * 
 * Copyright (C) 2015 Mohammad Javad Dousti, Qing Xie, and Massoud Pedram, SPORT lab, 
 * University of Southern California. All rights reserved.
 * 
 * Please refer to the LICENSE file for terms of use.
 * 
*/

#include "headers/general.h"
#include "headers/parser.h"
#include "headers/device.h"
#include "headers/model.h"
#include <time.h>

void printUsage(char * argv0){
	cerr <<"usage: therminator -d <file> -p <file> -o <file> [-t]"<<endl;
	cerr <<"Therminator v2: A fast thermal simulator for portable devices"<<endl;

	cerr <<" -d <file>\tInput design specification file"<<endl;
	cerr <<" -p <file>\tInput power trace file"<<endl;
	cerr <<" -o <file>\tOutput file"<<endl;
	cerr <<" -t\t\tTransient analysis"<<endl;
	cerr <<" -h\t\tShows this help menu"<<endl;
	exit(-1);
}

void parseCMD(int argc, char** argv, string &file_output, string &file_design, string &file_trace, bool &transient){
	bool flag_designfile = false;
	bool flag_tracefile = false;
	bool flag_outfile = false;

	if (argc <= 1 || argv[1] == string("-h") || argv[1] == string("-help") || argv[1] == string("--help")){
		printUsage(argv[0]);
	}

	transient = false;	// default is steady-state analysis
	/* First argument is the program name */
	for (int i = 1; i < argc; i++){
		if (argv[i] == string("-d")){
			flag_designfile = true;
			i++;
			file_design.assign(argv[i]);
		}else if (argv[i] == string("-p")){
			flag_tracefile = true;
			i++;
			file_trace.assign(argv[i]);
		}else if (argv[i] == string("-o")){
			flag_outfile = true;
			i++;
			file_output.assign(argv[i]);
		}else if (argv[i] == string("-t")){
			transient = true;
		}else{
			cerr<<"Option "<<argv[i]<<" is not supported."<<endl;
			printUsage(argv[0]);
		}
	}
	if (flag_designfile == false || flag_tracefile == false || flag_outfile == false){
		printUsage(argv[0]);
	}
}

Model* initModel(bool transient, string file_design, string file_trace){
	Device* device=Parser::parseDevice(file_design, file_trace);
	if (device == NULL){
		perror("Failed to create a device intance.\n");
		exit(-1);
	}

	Model *model=new Model(device, transient);
	model->makeResistanceModel();

//  model->printGMatrix();
//	exit(0);

    if (transient){
        model->makeCapacitanceModel();
//        model->printCVector();
    }
	return model;
}

int main(int argc, char** argv){
	struct timespec start, end;
	double elapsed;
	string file_output, file_design, file_trace;
	bool transient;
	vector<string> fileWaitList;
	string action;
	int requiredTempCount=0;

	//Recording the start time of the program
	clock_gettime(CLOCK_MONOTONIC, &start);


//  file_output = "output";
//  file_design = "/media/d_drive/Therminator/src/examples/package_GS4.xml";
//  file_trace = "/media/d_drive/Therminator/src/examples/power_GS4.trace";
//  transient = false;

	/* Parsing the input parameters */
	parseCMD(argc, argv, file_output, file_design, file_trace, transient);


	if (! Utils::fileExists ("init"))
		Utils::waitOnFile("init");
	
	Model* model= initModel(transient, file_design, file_trace);

	if(remove( "init" ) != 0 ){
		perror( "Error deleting file init.\n");
		exit(-1);
	}

	fileWaitList.push_back("finish");
	fileWaitList.push_back("init_power");
	fileWaitList.push_back("next");	

	do{
		action =Utils::waitOnFile(fileWaitList);
	
		if (action.compare("init_power")==0){
			requiredTempCount = model->read_power();
//			cout<<"Count: "<<requiredTempCount<<endl;
			clock_gettime(CLOCK_MONOTONIC, &start);
			model->solveSteadyState();
			model->printComponentTemp(file_output);
			cout<<"Solved and wrote to file"<<endl;
		}else if (action.compare("next")==0 && transient){

			int count = model->read_power();
			if (count!=requiredTempCount){
				cerr<<"Expected to get "<<requiredTempCount<<" power traces, but got "<<count<<"."<<endl;
				return -1;
			}
			model->solveTransientState();
			model->printComponentTemp(file_output);
			cout<<"Solved and wrote to file"<<endl;
		}else if (action.compare("next")==0){
			cout<<"You must use the transient option to perform transient simulation"<<endl;
		}

		if(remove(action.c_str()) != 0 ){
	    		perror( "Error deleting file.\n");
	        	exit(-1);
    	}
	}while(action.compare("finish")!=0);
	

//	model->printPVector();

	//Recording the end time of the program
	clock_gettime(CLOCK_MONOTONIC, &end);
	elapsed = (end.tv_sec - start.tv_sec);
	elapsed += (end.tv_nsec - start.tv_nsec) / 1e9;

	//model->printPVector();
	//double* T=model->findT();

	delete(model);
	cout<<endl<<"The program is finished successfully."<<endl;
	cout<<endl<<"Therminator runtime: "<<elapsed<<" s"<<endl;
	return 0;
}

