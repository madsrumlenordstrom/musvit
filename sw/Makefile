-include config.mk

# Directories
APPDIR   = $(CURDIR)/app
SRCDIR   = $(CURDIR)/src
INCDIR   = $(CURDIR)/inc
LIBDIR   = $(CURDIR)/lib
BUILDDIR = $(CURDIR)/build

# List of source og object files
APPS = $(subst /,,$(subst $(APPDIR),,$(wildcard $(APPDIR)/*/)))
SRCS = $(wildcard $(SRCDIR)/*.[cS])
OBJS = $(subst .S,.o,$(subst .c,.o,$(subst $(SRCDIR),$(LIBDIR),$(SRCS))))

# Tool stuff
LD_SCRIPT         = linker.ld
LD_FLAGS          = -Wl,--gc-sections,-T,$(LD_SCRIPT)
TOOLCHAIN_PREFIX ?= riscv64-elf-
CC                = $(TOOLCHAIN_PREFIX)gcc
CC_WARNINGS       = -Wall -Wextra
CC_TARGET         = -march=rv32im -mabi=ilp32
CC_FLAGS          = -ffunction-sections -ffreestanding -nostdlib -nodefaultlibs -nostartfiles -msmall-data-limit=0
CC_CMD            = $(CC) $(CC_WARNINGS) $(CC_TARGET) $(LD_FLAGS) $(CC_FLAGS)

# Generates object files from C or assembly source
.PRECIOUS: $(LIBDIR)/%.o
$(LIBDIR)/%.o: $(SRCDIR)/%.[cS] $(LD_SCRIPT)
	@mkdir -p $(@D)
	$(CC_CMD) -c $< -o $@

# Generates ELF files from application
.PRECIOUS: $(BUILDDIR)/%.elf
$(BUILDDIR)/%.elf: $(APPDIR)/%/*.[cS] $(LD_SCRIPT) $(OBJS)
	@mkdir -p $(@D)
	$(CC_CMD) -I$(INCDIR) $(OBJS) $< -o $@

# Generates raw instructions in binary
.PRECIOUS: $(BUILDDIR)/%.bin
$(BUILDDIR)/%.bin: $(BUILDDIR)/%.elf
	$(TOOLCHAIN_PREFIX)objcopy -O binary $< $@

# Generates raw instructions in ASCII hexadecimal
.PRECIOUS: $(BUILDDIR)/%.hex
$(BUILDDIR)/%.hex: $(BUILDDIR)/%.bin
	hexdump -v -e '"%08x\n"' $< > $@

# Build elf, bin and hex
%: $(APPDIR)/%/ $(BUILDDIR)/%.elf $(BUILDDIR)/%.bin $(BUILDDIR)/%.hex 
	@echo "Building app $@"

# Build everything
:PHONY: all
all: $(APPS)

.PHONY: clean
clean:
	$(RM) -rf $(BUILDDIR) $(LIBDIR) $(BUILDDIR) $(BUILDDIR)

# For debugging make build
.PHONY: show
show:
	@echo 'APPDIR					' $(APPDIR)
	@echo 'SRCDIR					' $(SRCDIR)
	@echo 'INCDIR					' $(INCDIR)
	@echo 'LIBDIR					' $(LIBDIR)
	@echo 'BUILDDIR 				' $(BUILDDIR)
	@echo 'APPS 					' $(APPS)
	@echo 'SRCS 					' $(SRCS)
	@echo 'OBJS 					' $(OBJS)
	@echo 'LD_SCRIPT				' $(LD_SCRIPT)
	@echo 'LD_FLAGS 				' $(LD_FLAGS)
	@echo 'CC   					' $(CC)
	@echo 'CC_WARNINGS				' $(CC_WARNINGS)
	@echo 'CC_TARGET				' $(CC_TARGET)
	@echo 'CC_FLAGS 				' $(CC_FLAGS)
	@echo 'CC_CMD					' $(CC_CMD)
