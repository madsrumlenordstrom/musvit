.text
.global main
main:
    li a0, 0           # Fn
	li a1, 0           # F0
	li a2, 1           # F1
    li a3, 9999        # max
    li a7, 0           # for printing int
l0:
    add a0, a1, a2     # Fn = F0 + F1
    mv a1, a2          # F0 = F1
    mv a2, a0          # F1 = Fn
    bgt a0, a3, exit   # if(Fn > max) jump to exit
    ecall
    j l0
exit:
	li a7, 10
	ecall