OUTPUT_ARCH( "riscv" )
MEMORY
{
   ROM (rx)  : ORIGIN = 0x00000000, LENGTH = 0x00001000
   RAM (rw)  : ORIGIN = 0x00001000, LENGTH = 0x00001000
}
ENTRY( _start )
SECTIONS
{
   PROVIDE(__stack_top = ORIGIN(RAM) + LENGTH(RAM));
   PROVIDE(__global_pointer$ = ORIGIN(RAM) + LENGTH(RAM));

   .init : {
      . = ALIGN(4);
      *(.init)
      . = ALIGN(4);
   } > ROM

   .text : {
      . = ALIGN(4);
      *(.text)
      *(.text*)
      . = ALIGN(4);
   } > ROM

  .rodata :
  {
    . = ALIGN(4);
    *(.rodata)
    *(.rodata*)
    . = ALIGN(4);
  } > ROM

   .data : {
      . = ALIGN(4);
      *(.data)
      *(.data*)
      . = ALIGN(4);
   } > RAM

   .bss : {
      . = ALIGN(4);
      *(.bss)
      . = ALIGN(4);
   } > RAM
}
