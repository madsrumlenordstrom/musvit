.text
.global main
main:
    li x10, 0            # a
    li x11, 0xCAFEBABE   # b
    li x12, 0xDEADBEEF   # c
    li x13, 64           # iterations
loop:
    addi x10, x10, 1
    and  x14, x11, x12
    xor  x15, x11, x12
    blt  x10, x13, loop  # a < 64
    li   a7,  3
    ecall
	li   a7,  10
	ecall