.text
.global main
main:
    li a0, 0           # Fn
	li a1, 0           # F0
	li a2, 1           # F1
    li a3, 9999        # max
    li a4, 10000000    # Delay cycles
    li a7, 0           # for printing int
l0:
    add a0, a1, a2     # Fn = F0 + F1
    mv a1, a2          # F0 = F1
    mv a2, a0          # F1 = Fn
    bgt a0, a3, main   # if(Fn > max) jump to exit
    ecall
    li a5, 0
l1:
    addi a5, a5, 1
    bltu a5, a4, l1
    j l0
exit:
	li a7, 10
	ecall