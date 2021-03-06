//------------------------------------------------------------------------------
// Sixi PID firmware
// 2020-03-28 dan@marginallyclever.com
// CC-by-NC-SA
//------------------------------------------------------------------------------

#include "configure.h"

MotorManager motorManager;

StepperMotor motors[NUM_AXIES];

#if NUM_SERVOS>0
Servo servos[NUM_SERVOS];
#endif


void StepperMotor::report() {
  //*
  Serial.print(letter);
  //Serial.print("\tpid=");  Serial.print(kp);
  //Serial.print(", ");      Serial.print(ki);
  //Serial.print(", ");      Serial.print(kd);
  //Serial.print("\tat");      Serial.print(angleTarget);
  //Serial.print("\tst");      Serial.print(stepsTarget);
  //Serial.print("\tsn");      Serial.print(stepsNow);
  //Serial.print("\te");       Serial.print(error);
  //Serial.print("\tv");
  Serial.print(velocityActual);
  //Serial.print("\tv");       Serial.print(velocityTarget);
  //Serial.println();
  //*/
}


void StepperMotor::updatePID(int32_t measuredSteps) {
  // use a PID to control the motion.

  //Serial.print(stepsTarget);  Serial.print(' ');
  //Serial.print(measuredSteps);  Serial.print(' ');
  //Serial.println();
  
  // P term
  error = stepsTarget - measuredSteps;
  
  interpolationTime += sPerTickISR;
  if( interpolationTime > totalTime ) interpolationTime = totalTime;
  
  float vInterpolated;
  if(totalTime>0) {
    vInterpolated = velocityOld + ( velocityTarget - velocityOld ) * interpolationTime / totalTime;
  } else {
    vInterpolated = velocityTarget;
  }

  // i term
  error_i += error * sPerTickISR;
  // d term
  //float error_d = (error - error_last) / sPerTickISR;
  // put it all together
  float positionInfluence = kp * ( error + ki * error_i );
  //float positionInfluence = kp * ( error + ki * error_i + kd * error_d );
  float newVel = vInterpolated + positionInfluence;
  
  error_last = error;
    
  // cap acceleration
  float acc=newVel-velocityActual;
  float maxA = motorManager.acceleration;
  if(fabs(acc)>maxA) {
    float ratio = maxA/abs(acc);
    acc*=ratio;
  }
  velocityActual += acc;

  //if(abs(error) < 0.5) velocityActual = 0;

  // stepInterval_us and stepDirection are used by the main loop and the ISR.
  // if the main loop is writing to those values when the ISR fires, the write is interrupted and the value the ISR reads could be junk.
  // my solution is to have two sets of values and a flag.  the main loop adjusts the flag after setting up the new values.
  // it's a bit like a lock.

  // get the next flag value
  uint8_t next = NEXT_PLANNER_STEP(currentPlannerStep);

  //
  stepsNow[next] = measuredSteps;
  
  if(abs(velocityActual) == 0) {
    stepInterval_us[next] = 0xFFFFFFFF;  // uint32_t max value
    timeSinceLastStep_us=0;
  } else {
    stepInterval_us[next] = 1000000.0 / abs(velocityActual);
    stepDirection[next] = velocityActual<0 ? -1:1;
    digitalWrite( dir_pin, velocityActual<0 ? HIGH : LOW );
  }
  // change the flag  
  currentPlannerStep=next;
}
  
// CAN'T PRINT INSIDE ISR 
// dt = us
void StepperMotor::ISRStepNow() {    
  timeSinceLastStep_us += usPerTickISR;

  if( timeSinceLastStep_us >= stepInterval_us[currentPlannerStep] ) {
    //if(!IS_DRYRUN)
    {
      stepsNow[currentPlannerStep] += stepDirection[currentPlannerStep];
      digitalWrite( step_pin, HIGH );
      digitalWrite( step_pin, LOW  );
    }
    timeSinceLastStep_us -= stepInterval_us[currentPlannerStep];
  }
}


void MotorManager::setup() {
#define SMP(LL,NN) { motors[NN].letter      = LL; \
                     motors[NN].step_pin    = MOTOR_##NN##_STEP_PIN; \
                     motors[NN].dir_pin     = MOTOR_##NN##_DIR_PIN; \
                     motors[NN].enable_pin  = MOTOR_##NN##_ENABLE_PIN; \
                     motors[NN].absMaxSteps = MOTOR_##NN##_STEPS_PER_TURN/2; \
                     motors[NN].limitMax    = DH_##NN##_MAX; \
                     motors[NN].limitMin    = DH_##NN##_MIN; }
  SMP('X',0)
  SMP('Y',1)
  SMP('Z',2)
  SMP('U',3)
  SMP('V',4)
  SMP('W',5)

  for(ALL_MOTORS(i)) {
    // set the motor pin & scale
    pinMode(motors[i].step_pin, OUTPUT);
    pinMode(motors[i].dir_pin, OUTPUT);
    pinMode(motors[i].enable_pin, OUTPUT);
    motors[i].totalTime=0;
  }

  // setup servos
#if NUM_SERVOS>0
  servos[0].attach(SERVO0_PIN);
#endif

  acceleration = DEFAULT_ACCELERATION;
}
