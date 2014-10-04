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

/*
 * 
 */
void main() {
    
    // Disable comparators
//    CM1CON = 0;
//    CM2CON = 0;
//    CM3CON = 0;
//    // Configure PORTA pins to be digital
//    ANSELA = 0;
//    // Specify PORTA as outputs
//    TRISAbits.TRISA0 = 0;
//    // Turn on LED
//    LATAbits.LATA0 = 1;

    //mPORTBDirection(0);

    mPORTAClearBits(BIT_0);           //Clear bits to ensure the LED is off.
    mPORTASetPinsDigitalOut(BIT_0);   //Set port as output

    int j;

    while(1)
    {
        j = 100000;
        mPORTAToggleBits(BIT_0);      //Toggle light status.
        while(j--) {}                 //Kill time.
    }

//    while (1)
//    {
//        //mPORTAToggleBits(BIT_0);
//    }

    return (EXIT_SUCCESS);
}

