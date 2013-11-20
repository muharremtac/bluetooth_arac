#include <NewSoftSerial.h>   //Software Serial Port
#define RxD 6
#define TxD 7
 
#define DEBUG_ENABLED  1
 
NewSoftSerial blueToothSerial(RxD,TxD);

char motorHizi=0;
int arkaMotor = 3;
int direksiyon = 11;

void setup() {
  Serial.begin(9600); 
  pinMode(RxD, INPUT);
  pinMode(TxD, OUTPUT); 
 
//  pinMode(arkaMotor, OUTPUT);
//  pinMode(direksiyon, OUTPUT);  

  pinMode(12, OUTPUT);
  pinMode(9, OUTPUT);
  
  pinMode(13, OUTPUT);
  pinMode(8, OUTPUT);

 analogWrite(arkaMotor, 0);  
 analogWrite(direksiyon, 0);    
  setupBlueToothConnection(); 
}

void loop() {

  while (!blueToothSerial.available());
  motorHizi = blueToothSerial.read(); 
  if( motorHizi == '5' ){
      digitalWrite(12, LOW);
      digitalWrite(9, LOW);
      analogWrite(arkaMotor, 150);   
  }
  else if( motorHizi == '4' ){
      digitalWrite(12, LOW);
      digitalWrite(9, LOW);
      analogWrite(arkaMotor, 200);   
  }
  else if( motorHizi == '3' ){
      digitalWrite(12, LOW);
      digitalWrite(9, LOW);
      analogWrite(arkaMotor,150);   
  }
  else if( motorHizi == '2' ){
      digitalWrite(12, LOW);
      digitalWrite(9, LOW);
      analogWrite(arkaMotor, 100);   
  }

  else if( motorHizi == '1' ){
      digitalWrite(12, HIGH);
      digitalWrite(9, LOW);
      analogWrite(arkaMotor, 75);   
  }

  else if( motorHizi == '0' ){
      analogWrite(arkaMotor, 0);   
  }

 //on motor ayarlari dur 
  else if( motorHizi == 'a' ){
    analogWrite(direksiyon, 0);   
  } 
 //on motor ayarlari saga 
  else if( motorHizi == 'b' ){
    digitalWrite(13, LOW);
    digitalWrite(8, LOW);
    analogWrite(direksiyon, 255);   
  } 
 //on motor ayarlari sola 
  else if( motorHizi == 'c' ){
    digitalWrite(13, HIGH);
    digitalWrite(8, LOW);
    analogWrite(direksiyon, 255);   
  } 
  else if( motorHizi == 'g' ){
     analogWrite(arkaMotor, 0);  
     analogWrite(direksiyon, 0);    
  } 
  else{
     analogWrite(arkaMotor, 0);  
     analogWrite(direksiyon, 0);    
  }
   
}

void setupBlueToothConnection()
{
  blueToothSerial.begin(38400); //Set BluetoothBee BaudRate to default baud rate 38400
  blueToothSerial.print("\r\n+STWMOD=0\r\n"); //set the bluetooth work in slave mode
  blueToothSerial.print("\r\n+STNA=SeeedBTSlave\r\n"); //set the bluetooth name as "SeeedBTSlave"
  blueToothSerial.print("\r\n+STOAUT=1\r\n"); // Permit Paired device to connect me
  blueToothSerial.print("\r\n+STAUTO=0\r\n"); // Auto-connection should be forbidden here
  delay(2000); // This delay is required.
  blueToothSerial.print("\r\n+INQ=1\r\n"); //make the slave bluetooth inquirable 
  Serial.println("The slave bluetooth is inquirable!");
  delay(2000); // This delay is required.
  blueToothSerial.flush();
}


