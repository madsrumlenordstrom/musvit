-include config.mk

BOARDS = $(subst /,,$(subst $(CURDIR),,$(wildcard $(CURDIR)/*/)))

.PHONY: synth
synth:
	$(MAKE) -C $(BOARD) synth TOPMOD=$(TOPMOD) RTLFILE=$(RTLFILE)

.PHONY: program
program:
	$(MAKE) -C $(BOARD) program TOPMOD=$(TOPMOD) RTLFILE=$(RTLFILE)

.PHONY: all
all:
	$(foreach board,$(BOARDS), $(MAKE) -C $(board) synth TOPMOD=$(TOPMOD) RTLFILE=$(RTLFILE))

.PHONY: clean
clean:
	$(foreach board,$(BOARDS), $(MAKE) -C $(board) clean)
