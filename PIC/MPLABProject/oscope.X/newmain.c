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
#include <peripheral/uart.h>


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

// Defines
#define SYSCLK 40000000L
#define PB_CLOCK 20000000

int SerialTransmit(const char *buffer);
unsigned int SerialReceive(char *buffer, unsigned int max_size);

int analogRead(char analogPIN){
    AD1CHS = analogPIN << 16;       // AD1CHS<16:19> controls which analog pin goes to the ADC

    AD1CON1bits.SAMP = 1;           // Begin sampling
    while( AD1CON1bits.SAMP );      // wait until acquisition is done
    while( ! AD1CON1bits.DONE );    // wait until conversion done

    return ADC1BUF0;                // result stored in ADC1BUF0
}

void adcConfigureManual(){
    AD1CON1CLR = 0x8000;    // disable ADC before configuration

    AD1CON1 = 0x00E0;       // internal counter ends sampling and starts conversion (auto-convert), manual sample
    AD1CON2 = 0;            // AD1CON2<15:13> set voltage reference to pins AVSS/AVDD
    AD1CON3 = 0x0f01;       // TAD = 4*TPB, acquisition time = 15*TAD
} // END adcConfigureManual()

void main() {
    DBINIT(); //Initialize the IO channel
    // Configure pins as analog inputs
    ANSELBbits.ANSB3 = 1;   // set RB3 (AN5) to analog
    TRISBbits.TRISB3 = 1;   // set RB3 as an input
    TRISBbits.TRISB5 = 0;   // set RB5 as an output (note RB5 is a digital only pin)
    adcConfigureManual();   // Configure ADC
    AD1CON1SET = 0x8000;    // Enable ADC

     // UART Pin Selections
    U1RXRbits.U1RXR = 3;    //SET RX to RB13
    RPB15Rbits.RPB15R = 1;    //SET RB15 to TX

    mPORTBSetPinsDigitalOut(BIT_15);      // Set PB15(Tx) as output
    mPORTBSetPinsDigitalIn (BIT_13);      // Set PB13(Rx) as input
    //int PB_CLOCK = 20000000;
    int desiredBAUD = 47020;

    // Calculate actual assigned baud rate
    int actual_baud = PB_CLOCK / (4 * desiredBAUD) - 1;

    OpenUART1(UART_EN|UART_BRGH_FOUR, UART_RX_ENABLE | UART_TX_ENABLE, actual_baud);

    char buffer[64];
    strcpy(buffer, "test");
    //itoa(buffer, variable, 16);
    //memset(filename,0,1024*sizeof(char));         //Clears the Array
    //printf(buffer, "Boscope Test");    //Places the String into the Array
    //putsUART1(filename);                        //Sends the Array over UART1
    // Small initial delay
    int t;
    for( t=0 ; t < 100000 ; t++);
    int variable = 0;
    int index = 0;
    while (1)
    {
        variable = analogRead(5);

        while( ! AD1CON1bits.DONE );
        memset(buffer,0,sizeof(buffer)); // Clear buffer
        sprintf(buffer, "%d\r\n",variable);
        //while(!UARTTransmitterIsReady(UART1)){};
        putsUART1(buffer);

        for(index=0;index<500000;index++) {
            Nop();
        }//a dummy function ot delay the loop
    }
    
}

//memset(buffer,0,sizeof(buffer)); // Clear buffer
//itoa(buffer, variable, 16);