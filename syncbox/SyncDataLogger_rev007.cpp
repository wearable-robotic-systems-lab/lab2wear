// Read Sync Input and Log Data
// rev001 - 10/2023

#define TRUE 1
#define FALSE 0

#include <iostream>
#include <fstream>
#include <mutex>
#include <thread>
#include <condition_variable>
#include <chrono>
#include <cstring>
#include <sstream>
#include <netdb.h>
#include <sys/types.h> 
#include <sys/socket.h> 
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <inttypes.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include <time.h>
#include <signal.h>
#include <sched.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/select.h>
#include <math.h>
#include <vector>
#include <numeric>
#include <algorithm>

// WiringPi libraries
#include <wiringPi.h>
#include <wiringSerial.h>
#include <wiringPiI2C.h>
#include <termios.h>


// DEFINE PACKET LENGTH
#define LENGTH_ANDROID_PACKET 47 // 3-Header / 1-Cmd / 8-T1, 8-T2, 8-T3, 8-T4, 8-androidSendRate / 3-Tail
#define LENGTH_SINGLE_PACKET 97 //81 // 3-Header / 8-SyncTime / 1-S1, 1-S2, 1-S3 / 8-T1, 8-T2, 8-T3, 8-T4, 8-SBC_sendRate, 8-SBC_receiveRate, 8-androidSendRate/ 3-Tail
#define NUMBER_BUFFER_PACKET 1000 // 1ms interval (1000 Hz)
#define LENGTH_BUFFER_BLOCK 73000 // LENGTH_SINGLE_PACKET * NUMBER_BUFFER_PACKET

// DEFINE PINS (BCM)
#define INPUT1 2
#define INPUT2 3
#define INPUT3 4
#define LED1 14
#define LED2 15
#define LED3 18

#define BUFFER 4096

using namespace std;

volatile bool syncSignal1 = 0;
volatile bool syncSignal2 = 0;
volatile bool syncSignal3 = 0;

bool recState;
// thread recThread;
// thread udpThread;
uint64_t startTime;
uint64_t intTime1;
uint64_t intTime2;
uint64_t deltaT;
uint64_t syncTime1;
uint64_t syncTime2;
uint64_t syncTime3;
uint64_t T1;
uint64_t T1_int;
uint64_t T2;
uint64_t T3;
uint64_t T4;
uint64_t androidSendTime;
uint64_t resetStartTime =0;
// uint64_t resetPeriod = 120*1000000;
// uint64_t catchTime = 30*1000000;
// std::vector<uint64_t> delayMatrix;
// std::vector<uint64_t> offsetMatrix;
// uint64_t T1_update;
// uint64_t T4_update;
// uint64_t syncTime_update;
uint64_t minimum_offset = 0;
bool offsetFlag = false;
uint64_t beginSend;
uint64_t endSend;
uint64_t sendTime;
uint64_t beginReceive;
uint64_t endReceive;
uint64_t receiveTime;

uint8_t commandByte;
uint8_t currBuff;
uint8_t prevBuff;

uint64_t recvWaitTime;

bool readyToWrite;

bool recordState;
bool standbyState;

uint8_t singleLogPacket[LENGTH_SINGLE_PACKET];

int sockfd;

mutex dataMutex[2];
mutex writeMutex;
mutex UDPMutex;
mutex UDPAndroidMutex;
mutex UDPAndroidP2PMutex;
condition_variable condWrite;
condition_variable condSend;

std::string fileName;

ofstream dataFile;


#define SA struct sockaddr


// GET SYSTEM TIME (MICROSECOND)
uint64_t getMicrosTimeStamp() 
{
  struct timeval tv;
  gettimeofday(&tv,NULL);
  return tv.tv_sec*(uint64_t)1000000+tv.tv_usec;
}



//////////////////////// ANDROID STUFFS
struct structUDPAndroid
{
	string ipAddress;
	string name;
	int port;
	uint8_t dataPacket[LENGTH_ANDROID_PACKET];
	unsigned int packetError;
	unsigned int packetReceived;
	uint8_t id;
};

struct structUDPStat 
{
	unsigned int packetUDPSent;
	unsigned int packetUDPReceived;
};

struct structAndroidPacket
{
	uint8_t commandByte;
	uint64_t T1;
	uint64_t T2;
	uint64_t T3;
	uint64_t T4;
	uint64_t androidSendRate;
};

// TO UNPACK PACKET RECEIVED FROM ANDROID
inline void reconstructAndroidPacket(uint8_t* recvbuffer, structAndroidPacket &dataAndroidPacket)
{
	uint8_t *pointer;

	// Header
	//recvbuffer[0];
	//recvbuffer[1];
	//recvbuffer[2];

	/////////////////////////////////////////////

	// Android command byte
	pointer = (uint8_t*)&dataAndroidPacket.commandByte;
	pointer[0] = recvbuffer[3];

	// T1
	pointer = (uint8_t*)&dataAndroidPacket.T1;
	pointer[0] = recvbuffer[4];
	pointer[1] = recvbuffer[5];
	pointer[2] = recvbuffer[6];
	pointer[3] = recvbuffer[7];
	pointer[4] = recvbuffer[8];
	pointer[5] = recvbuffer[9];
	pointer[6] = recvbuffer[10];
	pointer[7] = recvbuffer[11];

	// T2
	pointer = (uint8_t*)&dataAndroidPacket.T2;
	pointer[0] = recvbuffer[12];
	pointer[1] = recvbuffer[13];
	pointer[2] = recvbuffer[14];
	pointer[3] = recvbuffer[15];
	pointer[4] = recvbuffer[16];
	pointer[5] = recvbuffer[17];
	pointer[6] = recvbuffer[18];
	pointer[7] = recvbuffer[19];

	// T3
	pointer = (uint8_t*)&dataAndroidPacket.T3;
	pointer[0] = recvbuffer[20];
	pointer[1] = recvbuffer[21];
	pointer[2] = recvbuffer[22];
	pointer[3] = recvbuffer[23];
	pointer[4] = recvbuffer[24];
	pointer[5] = recvbuffer[25];
	pointer[6] = recvbuffer[26];
	pointer[7] = recvbuffer[27];

	// T4
	pointer = (uint8_t*)&dataAndroidPacket.T4;
	pointer[0] = recvbuffer[28];
	pointer[1] = recvbuffer[29];
	pointer[2] = recvbuffer[30];
	pointer[3] = recvbuffer[31];
	pointer[4] = recvbuffer[32];
	pointer[5] = recvbuffer[33];
	pointer[6] = recvbuffer[34];
	pointer[7] = recvbuffer[35];

	//Android send rate
	pointer = (uint8_t*)&dataAndroidPacket.androidSendRate;
	pointer[0] = recvbuffer[36];
	pointer[1] = recvbuffer[37];
	pointer[2] = recvbuffer[38];
	pointer[3] = recvbuffer[39];
	pointer[4] = recvbuffer[40];
	pointer[5] = recvbuffer[41];
	pointer[6] = recvbuffer[42];
	pointer[7] = recvbuffer[43];


	/////////////////////////////////////////////

	// Tail
	//recvbuffer[36];
	//recvbuffer[37];
	//recvbuffer[38];
}


// TO CREATE A REQUEST PACKET SENDING TO ANDROID
void createRequestTimePacket(uint8_t* buffer_out, uint64_t T1, uint64_t T2, uint64_t T3, uint64_t T4, uint8_t commandByte, uint64_t androidSendTime)
{
	uint8_t *pointer;

	// Header
	buffer_out[0]=0x01; 
	buffer_out[1]=0x02; 
	buffer_out[2]=0x03;

	// Android command byte
	buffer_out[3]=commandByte;

	// T1
	pointer=(uint8_t*)&T1;
	buffer_out[4]=pointer[7];
	buffer_out[5]=pointer[6];
	buffer_out[6]=pointer[5];
	buffer_out[7]=pointer[4];
	buffer_out[8]=pointer[3];
	buffer_out[9]=pointer[2];
	buffer_out[10]=pointer[1];
	buffer_out[11]=pointer[0];

	// T2
	pointer=(uint8_t*)&T2;
	buffer_out[12]=pointer[7];
	buffer_out[13]=pointer[6];
	buffer_out[14]=pointer[5];
	buffer_out[15]=pointer[4];
	buffer_out[16]=pointer[3];
	buffer_out[17]=pointer[2];
	buffer_out[18]=pointer[1];
	buffer_out[19]=pointer[0];

	// T3
	pointer=(uint8_t*)&T3;
	buffer_out[20]=pointer[7];
	buffer_out[21]=pointer[6];
	buffer_out[22]=pointer[5];
	buffer_out[23]=pointer[4];
	buffer_out[24]=pointer[3];
	buffer_out[25]=pointer[2];
	buffer_out[26]=pointer[1];
	buffer_out[27]=pointer[0];

	// T4
	pointer=(uint8_t*)&T4;
	buffer_out[28]=pointer[7];
	buffer_out[29]=pointer[6];
	buffer_out[30]=pointer[5];
	buffer_out[31]=pointer[4];
	buffer_out[32]=pointer[3];
	buffer_out[33]=pointer[2];
	buffer_out[34]=pointer[1];
	buffer_out[35]=pointer[0];

	// androidSendTime
	pointer=(uint8_t*)&androidSendTime;
	buffer_out[36]=pointer[7];
	buffer_out[37]=pointer[6];
	buffer_out[38]=pointer[5];
	buffer_out[39]=pointer[4];
	buffer_out[40]=pointer[3];
	buffer_out[41]=pointer[2];
	buffer_out[42]=pointer[1];
	buffer_out[43]=pointer[0];

	// Tail
	buffer_out[44]=0x04; 
	buffer_out[45]=0x05; 
	buffer_out[46]=0x06;
}




//////////////////////// CONSTRUCT LOG PACKET
inline void constructLogPacket(uint8_t *dataPacket, uint64_t syncTime1, uint64_t syncTime2, uint64_t syncTime3, 
    uint8_t syncSignal1, uint8_t syncSignal2, uint8_t syncSignal3,    
    uint64_t T1, uint64_t T2, uint64_t T3, uint64_t T4, uint64_t sendTime, uint64_t receiveTime, uint64_t minimum_offset,uint64_t androidSendRate)
{
    uint8_t *pointer;

    // HEADER
    ////////////////////////////////////////
    dataPacket[0] = 0x01;
    dataPacket[1] = 0x02;
    dataPacket[2] = 0x03;

    // INTERNAL TIMESTAMP
    ////////////////////////////////////////
    pointer = (uint8_t*)&syncTime1;
    dataPacket[3] = pointer[7];
    dataPacket[4] = pointer[6];
    dataPacket[5] = pointer[5];
    dataPacket[6] = pointer[4];
    dataPacket[7] = pointer[3];
    dataPacket[8] = pointer[2];
    dataPacket[9] = pointer[1];
    dataPacket[10] = pointer[0];
    
    pointer = (uint8_t*)&syncTime2;
    dataPacket[11] = pointer[7];
    dataPacket[12] = pointer[6];
    dataPacket[13] = pointer[5];
    dataPacket[14] = pointer[4];
    dataPacket[15] = pointer[3];
    dataPacket[16] = pointer[2];
    dataPacket[17] = pointer[1];
    dataPacket[18] = pointer[0];

    pointer = (uint8_t*)&syncTime3;
    dataPacket[19] = pointer[7];
    dataPacket[20] = pointer[6];
    dataPacket[21] = pointer[5];
    dataPacket[22] = pointer[4];
    dataPacket[23] = pointer[3];
    dataPacket[24] = pointer[2];
    dataPacket[25] = pointer[1];
    dataPacket[26] = pointer[0];

    // SYNC SIGNALS
    ////////////////////////////////////////
    dataPacket[27] = syncSignal1;
    dataPacket[28] = syncSignal2;
    dataPacket[29] = syncSignal3;

    // CRISTIAN'S FORMULA TIMESTAMPS
    ////////////////////////////////////////
    pointer = (uint8_t*)&T1;
    dataPacket[30] = pointer[7];
    dataPacket[31] = pointer[6];
    dataPacket[32] = pointer[5];
    dataPacket[33] = pointer[4];
    dataPacket[34] = pointer[3];
    dataPacket[35] = pointer[2];
    dataPacket[36] = pointer[1];
    dataPacket[37] = pointer[0];

    pointer = (uint8_t*)&T2;
    dataPacket[38] = pointer[7];
    dataPacket[39] = pointer[6];
    dataPacket[40] = pointer[5];
    dataPacket[41] = pointer[4];
    dataPacket[42] = pointer[3];
    dataPacket[43] = pointer[2];
    dataPacket[44] = pointer[1];
    dataPacket[45] = pointer[0];

    pointer = (uint8_t*)&T3;
    dataPacket[46] = pointer[7];
    dataPacket[47] = pointer[6];
    dataPacket[48] = pointer[5];
    dataPacket[49] = pointer[4];
    dataPacket[50] = pointer[3];
    dataPacket[51] = pointer[2];
    dataPacket[52] = pointer[1];
    dataPacket[53] = pointer[0];

    pointer = (uint8_t*)&T4;
    dataPacket[54] = pointer[7];
    dataPacket[55] = pointer[6];
    dataPacket[56] = pointer[5];
    dataPacket[57] = pointer[4];
    dataPacket[58] = pointer[3];
    dataPacket[59] = pointer[2];
    dataPacket[60] = pointer[1];
    dataPacket[61] = pointer[0];

    pointer = (uint8_t*)&sendTime;
    dataPacket[62] = pointer[7];
    dataPacket[63] = pointer[6];
    dataPacket[64] = pointer[5];
    dataPacket[65] = pointer[4];
    dataPacket[66] = pointer[3];
    dataPacket[67] = pointer[2];
    dataPacket[68] = pointer[1];
    dataPacket[69] = pointer[0];

	pointer = (uint8_t*)&receiveTime;
    dataPacket[70] = pointer[7];
    dataPacket[71] = pointer[6];
    dataPacket[72] = pointer[5];
    dataPacket[73] = pointer[4];
    dataPacket[74] = pointer[3];
    dataPacket[75] = pointer[2];
    dataPacket[76] = pointer[1];
    dataPacket[77] = pointer[0];

	pointer = (uint8_t*)&minimum_offset;
    dataPacket[78] = pointer[7];
    dataPacket[79] = pointer[6];
    dataPacket[80] = pointer[5];
    dataPacket[81] = pointer[4];
    dataPacket[82] = pointer[3];
    dataPacket[83] = pointer[2];
    dataPacket[84] = pointer[1];
    dataPacket[85] = pointer[0];
	
	pointer = (uint8_t*)&androidSendRate;
    dataPacket[86] = pointer[7];
    dataPacket[87] = pointer[6];
    dataPacket[88] = pointer[5];
    dataPacket[89] = pointer[4];
    dataPacket[90] = pointer[3];
    dataPacket[91] = pointer[2];
    dataPacket[92] = pointer[1];
    dataPacket[93] = pointer[0];

    // HEADER
    ////////////////////////////////////////
    dataPacket[94] = 0x04;
    dataPacket[95] = 0x05;
    dataPacket[96] = 0x06;
}



inline bool checkAndroidPacket(uint8_t *buffer,int ret)
{
  return (buffer[0]==0x01 && buffer[1]==0x02 && buffer[2]==0x03 && buffer[LENGTH_ANDROID_PACKET-3]==0x4 && buffer[LENGTH_ANDROID_PACKET-2]==0x5 && buffer[LENGTH_ANDROID_PACKET-1]==0x6 && ret==LENGTH_ANDROID_PACKET);
};


// void threadUDPReceiveEcho_withNTP(structUDPAndroid* threadUDPReceive)
// {
// 	bool UDPReceiveState = TRUE;
// 	UDPAndroidMutex.lock();  

// 	printf("Created UDP receiving echo (with NTP) thread! [%s IP: %s - port: %d]\n\n", threadUDPReceive->name.c_str(), threadUDPReceive->ipAddress.c_str(), threadUDPReceive->port);

// 	uint8_t id;
// 	int sockfdServer;
// 	socklen_t len;
// 	int ret;
// 	uint8_t recvBuffer[BUFFER];
// 	uint8_t returnBuffer[BUFFER];

// 	struct sockaddr_in addrServer;
// 	struct sockaddr_in addrClient;

// 	sockfdServer=socket(AF_INET,SOCK_DGRAM,IPPROTO_UDP);

// 	bzero(&addrServer,sizeof(addrServer));
// 	addrServer.sin_family = AF_INET;
// 	addrServer.sin_addr.s_addr= htonl(INADDR_ANY); // IP_ADDRESS(127, 0, 0, 1);
// 	addrServer.sin_port=htons(threadUDPReceive->port);
  
// 	bind(sockfdServer,(struct sockaddr *)&addrServer,sizeof(addrServer));

// 	len = (socklen_t)sizeof(addrClient);

// 	id=threadUDPReceive->id;
// 	UDPAndroidMutex.unlock();
// 	usleep(250);

// 	structAndroidPacket dataAndroidUDP;
// 	dataAndroidUDP.T1 = 0;
// 	dataAndroidUDP.T2 = 0;
// 	dataAndroidUDP.T3 = 0;
// 	dataAndroidUDP.T4 = 0;
// 	dataAndroidUDP.commandByte = 0;

// 	printf("Android UDP receiving thread created...\n\n");
// 	printf("Waiting for Android packet...\n\n");

// 	// while(UDPReceiveState == TRUE)
// 	while (TRUE)
// 	{   
// 		ret=recvfrom(sockfdServer,recvBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,&len);    
// 		if (ret>1)
// 		{      
// 		UDPAndroidMutex.lock();      
// 		threadUDPReceive->packetReceived++;
// 			if (checkAndroidPacket(recvBuffer,ret))
// 			{
// 				// Copy from receive buffer 
// 				memcpy(threadUDPReceive->dataPacket,recvBuffer,LENGTH_ANDROID_PACKET);

// 				// Here we reconstruct Android command packet
// 				reconstructAndroidPacket(threadUDPReceive->dataPacket,dataAndroidUDP);
// 				commandByte = dataAndroidUDP.commandByte;
// 				T1 = dataAndroidUDP.T1;
// 				T2 = getMicrosTimeStamp() - startTime;
// 				T3 = getMicrosTimeStamp() - startTime;
// 				// T4 = dataAndroidUDP.T4;

// 				createRequestTimePacket(returnBuffer,T1,T2,T3,0,commandByte);

// 				sendto(sockfdServer,returnBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,sizeof(addrClient)); // Send back

// 				if (commandByte == 2) 
// 				{
// 					fileName = to_string(dataAndroidUDP.T1) + "_SBC.dat";
// 					startTime = getMicrosTimeStamp();
// 				}

// 				if (commandByte  == 4)
// 				{        
// 					printf("Powering down...\n");
// 					system("sudo poweroff");
// 				}

// 				// cout << "Android UDP packet received: " << threadUDPReceive->packetReceived << "\n";
// 				cout << "T1: " << T1 << "  -  T2: " << T2 << "\n";
// 			}
// 			else
// 			{
// 				threadUDPReceive->packetError++;  
// 				printf("Ret = %d\n", ret);
// 			}      
// 			UDPAndroidMutex.unlock();      
// 		}

// 		if(commandByte == 3)
// 		{
// 			// UDPReceiveState = FALSE;
// 			threadUDPReceive->packetReceived = 0;
// 			printf("Reset packet counts in Android UDP thread...\n");
// 		}
// 	}
// 	threadUDPReceive->packetReceived = 0;
// 	printf("Exiting from Android UDP thread...\n");
// }





// void threadUDPSendEcho_withNTP(structUDPAndroid* threadUDPReceive)
// {
// 	bool UDPReceiveState = TRUE;
// 	UDPAndroidMutex.lock();  

// 	printf("Created UDP sending echo (NTP) thread! [%s IP: %s - port: %d]\n\n", threadUDPReceive->name.c_str(), threadUDPReceive->ipAddress.c_str(), threadUDPReceive->port);

// 	uint8_t id;
// 	int sockfdServer;
// 	socklen_t len;
// 	int ret;
// 	uint8_t recvBuffer[BUFFER];
// 	uint8_t sendBuffer[BUFFER];
// 	uint8_t returnBuffer[BUFFER];

// 	struct sockaddr_in addrServer;
// 	struct sockaddr_in addrClient;

// 	sockfdServer=socket(AF_INET,SOCK_DGRAM,IPPROTO_UDP);

// 	bzero(&addrServer,sizeof(addrServer));
// 	addrServer.sin_family = AF_INET;
// 	addrServer.sin_addr.s_addr= htonl(INADDR_ANY); // IP_ADDRESS(127, 0, 0, 1);
// 	addrServer.sin_port=htons(threadUDPReceive->port);
  
// 	bind(sockfdServer,(struct sockaddr *)&addrServer,sizeof(addrServer));

// 	len = (socklen_t)sizeof(addrClient);

// 	id=threadUDPReceive->id;
// 	UDPAndroidMutex.unlock();
// 	usleep(250);

// 	structAndroidPacket dataAndroidUDP;
// 	dataAndroidUDP.T1 = 0;
// 	dataAndroidUDP.T2 = 0;
// 	dataAndroidUDP.T3 = 0;
// 	dataAndroidUDP.T4 = 0;
// 	dataAndroidUDP.commandByte = 0;

// 	printf("Android UDP thread created...\n\n");
// 	// printf("Waiting for Android packet...\n\n");

// 	uint64_t threadTime;
// 	uint64_t threadTimeInterval;

// 	commandByte = 0;

// 	while (TRUE)
// 	{   
// 		// threadTime = getMicrosTimeStamp() / 1000;
// 		// if (recordState)
// 		// {
// 		// 	// cout << "Thread time: " << threadTime << " Thread interval: " << threadTimeInterval << endl;

// 		// 	// Only move forward at 1s interval
// 		// 	if ((threadTime - threadTimeInterval) >= 2000)
// 		// 	{
// 		// 		cout << "Interval (us):" << (threadTime - threadTimeInterval) << "\n";
// 		// 		threadTimeInterval = threadTime;

// 		// 		T1 = getMicrosTimeStamp() - startTime;

// 		// 		createRequestTimePacket(sendBuffer,T1,0,0,0,5); // Command byte 5
// 		// 		sendto(sockfdServer,sendBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,sizeof(addrClient)); // Send request

// 		// 		cout << "Request packet sent! " << "\n";
// 		// 	}

// 		// 	ret=recvfrom(sockfdServer,recvBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,&len);    
// 		// 	if (ret>1)
// 		// 	{      

// 		// 		if (checkAndroidPacket(recvBuffer,ret))
// 		// 		{
// 		// 			// Copy from receive buffer 
// 		// 			memcpy(threadUDPReceive->dataPacket,recvBuffer,LENGTH_ANDROID_PACKET);

// 		// 			// Here we reconstruct Android command packet
// 		// 			reconstructAndroidPacket(threadUDPReceive->dataPacket,dataAndroidUDP);
// 		// 			commandByte = dataAndroidUDP.commandByte;
// 		// 			T1 = dataAndroidUDP.T1;
// 		// 			T2 = dataAndroidUDP.T2;
// 		// 			T3 = dataAndroidUDP.T3;
// 		// 			T4 = getMicrosTimeStamp() - startTime;

// 		// 			cout << "T1 (SBC): " << T1 << "  -  T2 (Android): " << T2 << "\n\n\n";

// 		//             // Create log packet
// 		//             constructLogPacket(singleLogPacket, syncTime, syncSignal1, syncSignal2, syncSignal3, dataAndroidUDP.T1, dataAndroidUDP.T2, dataAndroidUDP.T3, T4);

// 		// 			// Open data file
// 		// 			dataFile.write((char*)singleLogPacket, LENGTH_SINGLE_PACKET);
// 		// 			dataFile.close();
// 		//             cout << "Log written! " << "\n\n";

// 		// 		}

// 		// 	}			

// 		// 	// Stopping command
// 		// 	if (commandByte == 3)
// 		// 	{
// 		// 		recordState = FALSE;
// 		// 		threadUDPReceive->packetReceived = 0;
// 		// 		printf("Reset packet counts in Android UDP thread...\n");
// 		// 	}

// 		// }
// 		// else
// 		// {
// 			ret=recvfrom(sockfdServer,recvBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,&len);    
// 			if (ret>1)
// 			{      

// 				if (checkAndroidPacket(recvBuffer,ret))
// 				{
// 					// Copy from receive buffer 
// 					memcpy(threadUDPReceive->dataPacket,recvBuffer,LENGTH_ANDROID_PACKET);

// 					// Here we reconstruct Android command packet
// 					reconstructAndroidPacket(threadUDPReceive->dataPacket,dataAndroidUDP);
// 					commandByte = dataAndroidUDP.commandByte;
// 					T1 = dataAndroidUDP.T1;
// 					T2 = dataAndroidUDP.T2;
// 					T3 = dataAndroidUDP.T3;
// 					T4 = getMicrosTimeStamp() - startTime;


// 					createRequestTimePacket(returnBuffer, 0, 0, 0, 0, commandByte);
// 					sendto(sockfdServer,returnBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,sizeof(addrClient)); // Send back
// 					cout << "T1: " << T1 << "  -  T2: " << T2 << "\n";


// 					// Starting command
// 					if (commandByte == 2) 
// 					{
// 						fileName = to_string(T2) + "_SBC.dat";
// 						startTime = getMicrosTimeStamp();
// 						cout << "File name: " << fileName << "\n";
// 						dataFile.open("/home/pi/FDA-SyncProject/logs/" + fileName, ios::app | ios::binary);

// 						recordState = TRUE;
// 						cout << "Thread time initiated!" << endl;
// 						threadTimeInterval = 0;
// 					}
// 				} 
// 			}


// 		// }


// 		// if (commandByte  == 4)
// 		// {        
// 		// 	printf("Powering down...\n");
// 		// 	system("sudo poweroff");
// 		// }

		

// 	}
// 	threadUDPReceive->packetReceived = 0;
// 	printf("Exiting from Android UDP thread...\n");
// }





// HARDWARE INTERUPT FUNCTIONS
void watchInterrupt1(void)
{
    syncSignal1 = !syncSignal1;
    digitalWrite(LED1, syncSignal1);    
    syncTime1 = getMicrosTimeStamp() - startTime;
    // syncTime = getMicrosTimeStamp();
    cout << "Sync 1: " << syncSignal1 << "   -   Time: " << syncTime1 << "\n";
}

void watchInterrupt2(void)
{
    syncSignal2 = !syncSignal2;
    digitalWrite(LED2, syncSignal2);  
    syncTime2 = getMicrosTimeStamp() - startTime;  
    // syncTime = getMicrosTimeStamp();  
    cout << "Sync 2: " << syncSignal2 << "   -   Time: " << syncTime2 << "\n";
}

void watchInterrupt3(void)
{
    syncSignal3 = !syncSignal3;
    digitalWrite(LED3, syncSignal3);   
    syncTime3 = getMicrosTimeStamp() - startTime;
    // syncTime = getMicrosTimeStamp();
    cout << "Sync 3: " << syncSignal3 << "   -   Time: " << syncTime3 << "\n";
}

// SOME BLINKING PATTERNS
void blinkRapid() 
{    
    for (int i = 0 ; i < 5 ; i++)
    {        
        digitalWrite(LED1, 0);
        digitalWrite(LED2, 0);
        digitalWrite(LED3, 0);
		this_thread::sleep_for(chrono::milliseconds(75));
        digitalWrite(LED1, 1);
        digitalWrite(LED2, 1);
        digitalWrite(LED3, 1);
		this_thread::sleep_for(chrono::milliseconds(75));
    }
    digitalWrite(LED1, 0);
    digitalWrite(LED2, 0);
    digitalWrite(LED3, 0);
}

void blinkSequential() 
{
	int pinLED[3] = {14, 15, 18};
	for (int i = 0 ; i < 3 ; i++)
	{
        digitalWrite(pinLED[i], 1);
		this_thread::sleep_for(chrono::milliseconds(75));
	}
	for (int i = 0 ; i < 3 ; i++)
	{
        digitalWrite(pinLED[i], 0);
		this_thread::sleep_for(chrono::milliseconds(75));
	}
    digitalWrite(LED1, 0);
    digitalWrite(LED2, 0);
    digitalWrite(LED3, 0);
}




void writeBufferBlockToFile(ofstream &dataFile, string fileName, uint8_t *bufferBlock0, uint8_t *bufferBlock1)
{ 
	// uint8_t prevBuff = 0;  
	printf("Thread write initialized!\n\n");
	bool writeState = true;

	while(writeState == true)
	{
		unique_lock<mutex> lck(dataMutex[prevBuff]);
		condWrite.wait(lck, []{return readyToWrite;});

		// dataFile.open("/home/pi/GattServer/src/DataLogs/" + fileName, ios::app | ios::binary);
		dataFile.open("/home/pi/FDA-SyncProject/logs/" + fileName, ios::app | ios::binary);

		if (prevBuff==0)
		{
			//pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, NULL);
			dataFile.write((char*)bufferBlock0, LENGTH_BUFFER_BLOCK);
			prevBuff=1;
		}
		else
		{
			//pthread_setcancelstate(PTHREAD_CANCEL_DISABLE, NULL);
			dataFile.write((char*)bufferBlock1, LENGTH_BUFFER_BLOCK);
			prevBuff=0;
		}
		// }
		//pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, NULL);
		dataFile.close();
		readyToWrite = false;

		if(commandByte == 3)
		{
			writeState = false;     
			lck.unlock(); 
		}
	}
	printf("Thread write releases lock and exits...\n\n");
	prevBuff = 0;
	return;
}




////////////////////////////////////////////////////
// MAIN CODE
int main(int argc, char* argv[])
{
    // INITIALIZE GPIO
    printf("Initializing GPIO...\n\n");
    wiringPiSetupGpio();

    // INITIATE INTERRUPTS
    if (wiringPiISR(INPUT1, INT_EDGE_BOTH, &watchInterrupt1) < 0)
    {
        fprintf(stderr, "Unable to setup ISR 1: %s\n", strerror(errno));
        return 1;
    }
    if (wiringPiISR(INPUT2, INT_EDGE_BOTH, &watchInterrupt2) < 0)
    {
        fprintf(stderr, "Unable to setup ISR 2: %s\n", strerror(errno));
        return 1;
    }
    if (wiringPiISR(INPUT3, INT_EDGE_BOTH, &watchInterrupt3) < 0)
    {
        fprintf(stderr, "Unable to setup ISR 3: %s\n", strerror(errno));
        return 1;
    }

    // DECLARE LED OUTPUTS
    pinMode(LED1, OUTPUT);
    pinMode(LED2, OUTPUT);
    pinMode(LED3, OUTPUT);
    digitalWrite(LED1, 0);
    digitalWrite(LED2, 0);
    digitalWrite(LED3, 0);
    printf("GPIO initialized successfully!\n\n");

    // SOME BLINKING
	blinkRapid();
	this_thread::sleep_for(chrono::milliseconds(200));
	blinkSequential();

    // INITIATE ANDROID INTERFACE FOR WIFI P2P
    structUDPAndroid AndroidUDP; 
    AndroidUDP.name = "Android Device";
    AndroidUDP.ipAddress = "192.168.49.1"; 
    AndroidUDP.port = 8810;
    AndroidUDP.packetError = 0;
    AndroidUDP.packetReceived = 0;
    AndroidUDP.id = 1;

    // INITIATE TIME VARIABLES
    // internalTime = 0;
    // syncTime = 0;
    // T1 = 0;
    // T2 = 0;
    // T3 = 0;
    // T4 = 0;
    // uint32_t currentTime = 0;

    cout << "Running data logger..." << endl;


	structAndroidPacket dataAndroidUDP;
	dataAndroidUDP.T1 = 0;
	dataAndroidUDP.T2 = 0;
	dataAndroidUDP.T3 = 0;
	dataAndroidUDP.T4 = 0;
	dataAndroidUDP.commandByte = 0;
	dataAndroidUDP.androidSendRate=0;


	// thread threadAndroidUDP;   
	// threadAndroidUDP = thread(threadUDPSendEcho_withNTP, &AndroidUDP);
	// threadAndroidUDP.detach(); 
	// this_thread::sleep_for(chrono::milliseconds(500));
	// printf("\n\nThread Android UDP created!\n\n");



	// SET UP SOCKET
	// UDPAndroidMutex.lock(); 
	// printf("Created UDP sending echo (NTP) thread! [%s IP: %s - port: %d]\n\n", threadUDPReceive->name.c_str(), threadUDPReceive->ipAddress.c_str(), threadUDPReceive->port);
	uint8_t id;
	int sockfdServer;
	socklen_t len;
	int ret;

	uint8_t receivedDataBuffer[BUFFER];

	struct sockaddr_in addrServer;
	struct sockaddr_in addrClient;

	sockfdServer=socket(AF_INET,SOCK_DGRAM,IPPROTO_UDP); 


	bzero(&addrServer,sizeof(addrServer));
	addrServer.sin_family = AF_INET;
	addrServer.sin_addr.s_addr= htonl(INADDR_ANY); // IP_ADDRESS(127, 0, 0, 1);
	addrServer.sin_port=htons(8810);
  
	bind(sockfdServer,(struct sockaddr *)&addrServer,sizeof(addrServer));

	len = (socklen_t)sizeof(addrClient);
	id=1;
	// UDPAndroidMutex.unlock();

    struct timeval timeout;
    timeout.tv_sec = 0; // Set timeout in seconds
    timeout.tv_usec = 500000; // Set timeout in microseconds

	setsockopt(sockfdServer, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof timeout);

	// cout << "Setting UDP socket to non-blocking..." << endl;
	// fcntl(sockfdServer, F_SETFL, O_NONBLOCK);
	// this_thread::sleep_for(chrono::milliseconds(100));


    // // Set the socket to non-blocking mode
    // if (fcntl(sockfdServer, F_SETFL, O_NONBLOCK) == -1) {
    //     cerr << "Error setting socket to non-blocking mode." << endl;
    //     close(sockfdServer);
    //     return -1;
    // }

	cout << "Android UDP initiated!" << endl;


	recordState = FALSE;
	standbyState = FALSE;


    // INITIATE VARIABLES FOR DATA FILE
    fileName = "testLog.dat";

	for (int i = 0; i < LENGTH_SINGLE_PACKET; i++)
	{
		singleLogPacket[i] = 0x00;
	}

	startTime = getMicrosTimeStamp();
	uint64_t timeInterval;

	bool bufferEmpty;



    // MAIN CODE
    while (TRUE)
    {
    	uint8_t recvBuffer[BUFFER];
		uint8_t sendBuffer[BUFFER];

    	switch (recordState) {
    		case false:
				ret=recvfrom(sockfdServer,recvBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,&len);    
				if (ret>1)
				{      
					if (checkAndroidPacket(recvBuffer,ret))
					{
						// Copy from receive buffer 
						memcpy(receivedDataBuffer,recvBuffer,LENGTH_ANDROID_PACKET);

						// Here we reconstruct Android command packet
						reconstructAndroidPacket(receivedDataBuffer,dataAndroidUDP);
						commandByte = dataAndroidUDP.commandByte;
						T1 = dataAndroidUDP.T1;
						T2 = dataAndroidUDP.T2;
						T3 = dataAndroidUDP.T3;
						T4 = getMicrosTimeStamp() - startTime;


						createRequestTimePacket(sendBuffer, dataAndroidUDP.T1, dataAndroidUDP.T2, 0, 0, commandByte,0);
						sendto(sockfdServer,sendBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,sizeof(addrClient)); // Send back
						cout << "T1: " << T1 << "  -  T2: " << T2 << "\n";

						// Starting command
						if (commandByte == 2) 
						{
							fileName = to_string(T2) + "_SBC.dat";
							// startTime = getMicrosTimeStamp();
							cout << "File name: " << fileName << "\n";

							recordState = TRUE;
							cout << "Time initiated!" << "\n";
							timeInterval = 0;
							this_thread::sleep_for(chrono::milliseconds(3000));
						}
					} 
				}
				break;

    		case true:
				// Stopping command
				if (commandByte == 3)
				{
					recordState = FALSE;
					// threadUDPReceive->packetReceived = 0;
					// printf("Reset packet counts in Android UDP thread...\n");
					// this_thread::sleep_for(chrono::milliseconds(3000));
				}

			    // fd_set readSet;
			    // FD_ZERO(&readSet);
			    // FD_SET(sockfdServer, &readSet);

			    // struct timeval timeout;
			    // timeout.tv_sec = 0; // Set timeout in seconds
			    // timeout.tv_usec = 500; // Set timeout in microseconds

			    // int timeoutResult = select(sockfdServer + 1, &readSet, nullptr, nullptr, &timeout);

				// intTime1 = getMicrosTimeStamp() - startTime;
				intTime1 = getMicrosTimeStamp();

				// Only move forward at 1s interval
				if ((intTime1 - intTime2) >= 1000000)
				{
					cout << "Interval (ms): " << (intTime1 - intTime2) << "\n";
					intTime2 = intTime1;

					// memset(sendBuffer, 0, sizeof(sendBuffer));
					T1_int = getMicrosTimeStamp() - startTime;
					cout << "Internal T1: " << T1_int << " ";
					// T1 = getMicrosTimeStamp();

					createRequestTimePacket(sendBuffer,T1_int,0,0,0,1,0); // Command byte 1
					beginSend = getMicrosTimeStamp() - startTime;
					sendto(sockfdServer,sendBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,sizeof(addrClient)); // Send request
					endSend = getMicrosTimeStamp() - startTime;
					cout << "Request packet sent! " << "\n";
					// this_thread::sleep_for(chrono::milliseconds(20));
					// }

					// if (FD_ISSET(sockfdServer, &readSet)) {

					// Flush receiving buffer
					// memset(recvBuffer, 0, sizeof(recvBuffer));
					beginReceive = getMicrosTimeStamp() - startTime;
					ret=recvfrom(sockfdServer,recvBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,&len);
					endReceive = getMicrosTimeStamp() - startTime;
					T4 = getMicrosTimeStamp() - startTime;
					// recvWaitTime = getMicrosTimeStamp();
					// while ((getMicrosTimeStamp() - recvWaitTime) < 500000)
					// {
						if (ret>1)
						{      
							if (checkAndroidPacket(recvBuffer,ret))
							{
								// Copy from receive buffer 
								memcpy(receivedDataBuffer,recvBuffer,LENGTH_ANDROID_PACKET);

								// Here we reconstruct Android command packet
								reconstructAndroidPacket(receivedDataBuffer,dataAndroidUDP);
								commandByte = dataAndroidUDP.commandByte;
								T1 = dataAndroidUDP.T1;
								T2 = dataAndroidUDP.T2;
								T3 = dataAndroidUDP.T3;
								// T4 = getMicrosTimeStamp() - startTime;
								androidSendTime = dataAndroidUDP.androidSendRate;
								
								sendTime = endSend-beginSend;
								receiveTime = endReceive-beginReceive;

								cout << "Return T1: " << T1 << "\n";

								if ((T1_int - T1) == 0) { // If internal T1 and return T1 are the same

									cout << "T1 (SBC): " << T1 << "  -  T2 (Android): " << T2 <<  "  -  Round trip (us): " << (T4 - T1_int) << "\n\n";
									if (T4-T1<=50*1000){
										minimum_offset = ((T2-T1)+(T3-T4))/2;
									}
									// T1_update = T1+minimum_offset;
									// T4_update = T4+minimum_offset;
									// syncTime_update = syncTime+minimum_offset;

						            // Create log packet
						            constructLogPacket(singleLogPacket, syncTime1, syncTime2, syncTime3, syncSignal1, syncSignal2, syncSignal3, T1, T2, T3, T4,sendTime,receiveTime,minimum_offset,androidSendTime);

									// Write to data file
									dataFile.open("/home/pi/FDA-SyncProject/logs/" + fileName, ios::app | ios::binary);
									dataFile.write((char*)singleLogPacket, LENGTH_SINGLE_PACKET);
									dataFile.close();

									cout << "Log written!" << "\n\n";
								} 
								else
								{
									cout << "Packet mismatched! Skipping..." << "\n\n";
									bufferEmpty = 0;
									while (!bufferEmpty) 
									{
										cout << "Emptying buffer..." << "\n";
										ret=recvfrom(sockfdServer,recvBuffer,LENGTH_ANDROID_PACKET,0,(struct sockaddr *)&addrClient,&len); // Read again to empty the buffer
										if (ret<1)
										{
											cout << "Buffer emptied! Go back to main loop..." << "\n\n";
											bufferEmpty = 1;
										}

									}

								}

							} 
						}
						// break;
					// }

					// }
				}
				break;

    	}



		// while (recordState)
		// {


		// 	// this_thread::sleep_for(chrono::milliseconds(2000));


		// }



	}

	printf("End of code!\n\n\n"); // Should never get here...

}

