# This code performs initialization work required before
# calling the main function of the program

.section .init, "ax"             # Put code in .init section
.global _start                   # Make _start visible to linker
_start:                          # Entry point for programs
    .cfi_startproc               # Signal start of function
    .cfi_undefined ra            # Do not restore ra register
    .option push
    .option norelax
    la gp, __global_pointer$     # Initialize global pointer
    .option pop
    la sp, __stack_top           # Initialize stack pointer
    add s0, sp, zero             # Initialize frame pointer
    j main                       # Go to main function
    .cfi_endproc                 # Signal end of funtion
    .end                         # Signal end of assembly
