-include config.mk

# Tools
SBT         = sbt
SBTFLAGS    = --client
GTKWAVE     = gtkwave

# Project definitions
TOPMOD      = Musvit
TOPPKG      = musvit
MAIN        = MusvitMain

# Directories
SRCDIR      = $(CURDIR)/src/main/scala
TESTDIR     = $(CURDIR)/src/test/scala
RTLDIR      = $(CURDIR)/rtl
SWDIR       = $(CURDIR)/sw
SYNTHDIR    = $(CURDIR)/synth
DIRS        = $(SRCDIR) $(TESTDIR) $(RTLDIR)

# Sources
SRCS        = $(shell find $(SRCDIR) -name '*.scala')
TESTS       = $(shell find $(TESTDIR) -name '*.scala')

# Targets (configured in config.mk)
RTLFILE    ?= $(RTLDIR)/$(TOPMOD).sv
MAINTARGET ?= $(TOPPKG).$(MAIN)
TESTTARGET ?= $(TOPPKG).$(TOPMOD)
APPTARGET  ?=
BOARD      ?=
CLOCKFREQ  ?=
WAVETARGET ?=
WAVECONFIG ?=

# For SystemVerilog generation
ROMFILE     = $(SWDIR)/build/$(APPTARGET).bin
NULL        =
SPACE       = $(NULL) #
COMMA       = ,
FIRRTLOPTS  = --target-dir $(RTLDIR)
FIRTOOLOPTS = --disable-all-randomization --strip-debug-info --lowering-options=disallowLocalVariables,disallowPackedArrays
# --strip-verilog -o ./firrtl -O release
RTLGENOPTS  = --rom-file $(ROMFILE) --clock-frequency $(CLOCKFREQ) --firrtl-opts \"$(FIRRTLOPTS)\" --firtool-opts \"$(FIRTOOLOPTS)\"

# Generic target names
.PHONY: all
all: $(RTLFILE) test

.PHONY: rtl
rtl: $(RTLFILE)

.PHONY: app
app: $(ROMFILE)

# Generate SystemVerilog
$(RTLFILE): $(SRCS) $(ROMFILE)
	$(SBT) $(SBTFLAGS) "runMain $(MAINTARGET) $(RTLGENOPTS)"

# Compile application
$(ROMFILE):
	$(MAKE) -C $(SWDIR) $(APPTARGET)

# Synthesize design
.PHONY: synth
synth: $(RTLFILE)
	$(MAKE) -C $(SYNTHDIR) synth BOARD=$(BOARD) TOPMOD=$(TOPMOD) RTLFILE=$(RTLFILE)

# Program FPGA
.PHONY: program
program: $(RTLFILE)
	$(MAKE) -C $(SYNTHDIR) program BOARD=$(BOARD) TOPMOD=$(TOPMOD) RTLFILE=$(RTLFILE)

# Run specific test
.PHONY: test
test: $(SRCS) $(TESTS)
	$(SBT) $(SBTFLAGS) testOnly "$(TESTTARGET)"

# Run all tests
.PHONY: testall
testall: $(SRCS) $(TESTS)
	$(SBT) $(SBTFLAGS) test

.PHONY: wave
wave:
	$(GTKWAVE) $(WAVETARGET) $(WAVECONFIG) &

# Create a file with random data (useful for testing)
random:
	dd if=/dev/random of=random count=8

# Shutdown SBT server
.PHONY: shutdown
shutdown:
	$(SBT) $(SBTFLAGS) shutdown

# Cleanup working directory
.PHONY: clean
clean: shutdown
	@echo "Cleaning workspace"
	$(RM) -rf $(RTLDIR) test_run_dir/ target/ project/target project/project random
	$(MAKE) -C $(SWDIR) clean
	$(MAKE) -C $(SYNTHDIR) clean

.PHONY: show
show:
	@echo 'SBT         :' $(SBT)
	@echo 'SBTFLAGS    :' $(SBTFLAGS)
	@echo 'GTKWAVE     :' $(GTKWAVE)
	@echo 'TOPPKG      :' $(TOPPKG)
	@echo 'MAIN        :' $(MAIN)
	@echo 'TOPMOD      :' $(TOPMOD)
	@echo 'SRCDIR      :' $(SRCDIR)
	@echo 'TESTDIR     :' $(TESTDIR)
	@echo 'RTLDIR      :' $(RTLDIR)
	@echo 'SWDIR       :' $(SWDIR)
	@echo 'SYNTHDIR    :' $(SYNTHDIR)
	@echo 'DIRS        :' $(DIRS)
	@echo 'SRCS        :' $(SRCS)
	@echo 'TESTS       :' $(TESTS)
	@echo 'RTLFILE     :' $(RTLFILE)
	@echo 'MAINTARGET  :' $(MAINTARGET)
	@echo 'TESTTARGET  :' $(TESTTARGET)
	@echo 'WAVETARGET  :' $(WAVETARGET)
	@echo 'WAVECONFIG  :' $(WAVECONFIG)
	@echo 'APPTARGET   :' $(APPTARGET)
	@echo 'BOARD       :' $(BOARD)
	@echo 'FIRRTLOPTS  :' $(FIRRTLOPTS)
	@echo 'FIRTOOLOPTS :' $(FIRTOOLOPTS)
	@echo 'RTLGENOPTS  :' $(RTLGENOPTS)


