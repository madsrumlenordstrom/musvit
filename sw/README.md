# Directory containing software
This directory contains software and a Makefile for building.
Libaries needed by basically all applications are located in the ```src/``` directory and their respective header files are located in ```inc/```.
Applicantions or individual programs are located in the ```app/``` directory where each sub-directory contains exactly one appplication.
To build an application run following command:
```
  make <app>
```
Here ```<app>``` is the name of any sub-directory contained in ```app/```.
This will create ```elf/```, ```bin/```, ```hex/``` and ```obj/```.
The respective directories will contain ac RISC-V ELF file, a binary file with RISC-V raw machine code, a hexadecimal ASCII file with RISC-V raw machine code and object files compiled from the ```src/``` directory.
You can build all applications  with:
```
  make
```
The toolchain prefix might be different on your machine which can cause build errors.
If you experience this you can create a file called ```config.mk``` where you define you own tool chain prefix.
For example:
```
  TOOLCHAIN_PREFIX := riscv32-unknown-elf-
```
