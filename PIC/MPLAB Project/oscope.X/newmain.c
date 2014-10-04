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
#define SYSCLK 40000000
#define PB_CLOCK 20000000

void main() {

     // Peripheral Pin Select
    U2RXRbits.U2RXR = 4;    //SET RX to RB8
    RPB9Rbits.RPB9R = 2;    //SET RB9 to TX
    //int PB_CLOCK = 20000000;
    int desiredBAUD = 9600;

    // Calculate actual assigned baud rate
    int actual_baud = PB_CLOCK / (4 * desiredBAUD) - 1;

    OpenUART2(UART_EN|UART_BRGH_FOUR, UART_RX_ENABLE | UART_TX_ENABLE, actual_baud);

    char filename[50] ;                         //Array of 50 chars
    memset(filename,0,50*sizeof(char));         //Clears the Array
    sprintf(filename, "Tutorial 4 ? BOSCOPE\nIncrementing Variable\n");    //Places the String into the Array
    putsUART2(filename);                        //Sends the Array over UART1

    int variable = 0;
    int index = 0;
    while (1)
    {
        sprintf(filename, "%d\n",variable); //%d will get the value of the next argument in the function and place it in the string
        putsUART2(filename);                //Sends the Array over UART1
        variable++;                         //Increments the variable by 1

        for(index=0;index<4000000;index++) {
            Nop();
        }//a dummy function ot delay the loop
    }
}