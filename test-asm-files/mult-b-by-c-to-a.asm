	LOAD R1, ZERO
	LOAD R2, C
START: 
	IFZER R2, END
	IFNEG R2, END
	ADD R1, B
	SUB R2, ONE
	GOTO START
END: 
	STORE R1, A
	STOP

A: 		0h
B: 		2h
C: 		AH
ZERO: 	0h
ONE: 	1h