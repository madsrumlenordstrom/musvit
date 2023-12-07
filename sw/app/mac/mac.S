.text
.global main
main:
    li x10, 0            # a
    li x11, 7            # b
    li x12, 3            # c
    li x13, 1340         # max
loop:
    mul  x14, x11, x12   # b * c
    add  x10, x14, x10   # a = a + (b * c)
    slt  x15, x10, x13   # a < max
    bnez x15, loop       # a < max != 0
    li   a7,  3
    ecall
	li   a7,  10
	ecall