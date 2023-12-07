.text
.global main
main:
    li x10, 0            # a
    li x11, 7            # x
    li x12, 0            # y
    li x13, 64           # iterations
loop:
    mul  x14, x11, x10   # x * a.
    add  x12, x14, x12   # x * a + y
    addi x10, x10, 1     # a = a + 1
    blt  x10, x13, loop  # a < 64
    li   a7,  3
    ecall
	li   a7,  10
	ecall