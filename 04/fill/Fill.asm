// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.

(LISTEN)
@KBD
D=M    // D=Key
@BLACK
D;JNE  // If any key is pressed goto BLACK

// fill white
@SCREEN
D=A
@place
M=D       // place=SCREEN
@i
M=1
(LOOP2)
@i
D=M       // D=i
@8192
D=D-A     // D=i-8192
@LISTEN
D;JGT     // If i>8192 goto LISTEN
@place
A=M       // D=place
M=0       // set M[place] to 0
@place
M=M+1
@i
M=M+1
@LOOP2
0;JMP

(BLACK)
// fill black
@SCREEN
D=A
@place
M=D       // place=SCREEN
@i
M=1
(LOOP1)
@i
D=M       // D=i
@8192
D=D-A     // D=i-8192
@LISTEN
D;JGT     // If i>8192 goto LISTEN
@place
A=M       // A=place
M=-1       // set M[place] to -1
@place
M=M+1
@i
M=M+1
@LOOP1
0;JMP
