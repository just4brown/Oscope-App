/* 
 * File:   newmain.c
 * Author: justinbrown
 *
 * Created on September 22, 2014, 3:40 PM
 */

#include <stdio.h>
#include <stdlib.h>
#include <p32xxxx.h>
#include <plib.h>

// DEVCFG1
#pragma config FNOSC = FRCDIV           // Oscillator Selection Bits (Fast RC Osc w/Div-by-N (FRCDIV))
#pragma config FSOSCEN = ON             // Secondary Oscillator Enable (Enabled)
#pragma config IESO = ON                // Internal/External Switch Over (Enabled)
#pragma config POSCMOD = HS             // Primary Oscillator Configuration (HS osc mode)
#pragma config OSCIOFNC = ON            // CLKO Output Signal Active on the OSCO Pin (Enabled)
#pragma config FPBDIV = DIV_1           // Peripheral Clock Divisor (Pb_Clk is Sys_Clk/1)
#pragma config FCKSM = CSDCMD           // Clock Switching and Monitor Selection (Clock Switch Disable, FSCM Disabled)
#pragma config WDTPS = PS1048576        // Watchdog Timer Postscaler (1:1048576)
#pragma config WINDIS = OFF             // Watchdog Timer Window Enable (Watchdog Timer is in Non-Window Mode)
#pragma config FWDTEN = ON              // Watchdog Timer Enable (WDT Enabled)
#pragma config FWDTWINSZ = WINSZ_25     // Watchdog Timer Window Size (Window Size is 25%)

// DEVCFG0
#pragma config JTAGEN = ON              // JTAG Enable (JTAG Port Enabled)
#pragma config ICESEL = ICS_PGx1        // ICE/ICD Comm Channel Select (Communicate on PGEC1/PGED1)
#pragma config PWP = OFF                // Program Flash Write Protect (Disable)
#pragma config BWP = OFF                // Boot Flash Write Protect bit (Protection Disabled)
#pragma config CP = OFF                 // Code Protect (Protection Disabled)

#define SYSCLK (40000000)

int analogRead(char analogPIN){
    AD1CHS = analogPIN << 16;       // AD1CHS<16:19> controls which analog pin goes to the ADC

    AD1CON1bits.SAMP = 1;           // Begin sampling
    while( AD1CON1bits.SAMP );      // wait until acquisition is done
    while( ! AD1CON1bits.DONE );    // wait until conversion done

    return ADC1BUF0;                // result stored in ADC1BUF0
}

void delay_us( unsigned t)          // See Timers tutorial for more info on this function
{
    T1CON = 0x8000;                 // enable Timer1, source PBCLK, 1:1 prescaler

    // delay 100us per loop until less than 100us remain
    while( t >= 100){
        t-=100;
        TMR1 = 0;
        while( TMR1 < SYSCLK/10000);
    }

    // delay 10us per loop until less than 10us remain
    while( t >= 10){
        t-=10;
        TMR1 = 0;
        while( TMR1 < SYSCLK/100000);
    }

    // delay 1us per loop until finished
    while( t > 0)
    {
        t--;
        TMR1 = 0;
        while( TMR1 < SYSCLK/1000000);
    }

    // turn off Timer1 so function is self-contained
    T1CONCLR = 0x8000;
} // END delay_us()

void adcConfigureManual(){
    AD1CON1CLR = 0x8000;    // disable ADC before configuration

    AD1CON1 = 0x00E0;       // internal counter ends sampling and starts conversion (auto-convert), manual sample
    AD1CON2 = 0;            // AD1CON2<15:13> set voltage reference to pins AVSS/AVDD
    AD1CON3 = 0x0f01;       // TAD = 4*TPB, acquisition time = 15*TAD
} // END adcConfigureManual()

int main( void)
{
	SYSTEMConfigPerformance(SYSCLK);

        // Configure pins as analog inputs
        ANSELBbits.ANSB3 = 1;   // set RB3 (AN5) to analog
        TRISBbits.TRISB3 = 1;   // set RB3 as an input
        TRISBbits.TRISB5 = 0;   // set RB5 as an output (note RB5 is a digital only pin)

        adcConfigureManual();   // Configure ADC
        AD1CON1SET = 0x8000;    // Enable ADC

        int foo;
	while ( 1)
	{
            foo = analogRead( 5); // note that we call pin AN5 (RB3) by it's analog number
            delay_us( foo);       // delay according to the voltage at RB3 (AN5)
            LATBINV = 0x0020;     // invert the state of RB5
	}

	return 0;
}