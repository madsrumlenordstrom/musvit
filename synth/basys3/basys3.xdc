## This file is a .xdc for the Basys3 rev B board
## Cut down to the Minimum for the Hello World example

## Clock signal
set_property PACKAGE_PIN W5 [get_ports clock]
set_property IOSTANDARD LVCMOS33 [get_ports clock]
create_clock -add -name sys_clk_pin -period 10.00 -waveform {0 5} [get_ports clock]
 
## reset button
set_property PACKAGE_PIN T17 [get_ports reset]
set_property IOSTANDARD LVCMOS33 [get_ports reset]

## Switches
set_property PACKAGE_PIN V17 [get_ports {io_autoCount}]
set_property PACKAGE_PIN V16 [get_ports {io_showUpper}]
set_property PACKAGE_PIN W16 [get_ports {io_switchDis}]

set_property IOSTANDARD LVCMOS33 [get_ports {io_autoCount}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_showUpper}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_switchDis}]

## Buttons
set_property PACKAGE_PIN T18 [get_ports {io_increment}]
set_property PACKAGE_PIN U17 [get_ports {io_decrement}]

set_property IOSTANDARD LVCMOS33 [get_ports {io_increment}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_decrement}]

##Seven segment display
set_property PACKAGE_PIN W7 [get_ports {io_seg[0]}]
set_property PACKAGE_PIN W6 [get_ports {io_seg[1]}]
set_property PACKAGE_PIN U8 [get_ports {io_seg[2]}]
set_property PACKAGE_PIN V8 [get_ports {io_seg[3]}]
set_property PACKAGE_PIN U5 [get_ports {io_seg[4]}]
set_property PACKAGE_PIN V5 [get_ports {io_seg[5]}]
set_property PACKAGE_PIN U7 [get_ports {io_seg[6]}]

set_property PACKAGE_PIN U2 [get_ports {io_an[0]}]
set_property PACKAGE_PIN U4 [get_ports {io_an[1]}]
set_property PACKAGE_PIN V4 [get_ports {io_an[2]}]
set_property PACKAGE_PIN W4 [get_ports {io_an[3]}]

set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[2]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[3]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[4]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[5]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_seg[6]}]

set_property IOSTANDARD LVCMOS33 [get_ports {io_an[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_an[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_an[2]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_an[3]}]

## LEDs
set_property PACKAGE_PIN U16 [get_ports {io_leds[0]}]
set_property PACKAGE_PIN E19 [get_ports {io_leds[1]}]
set_property PACKAGE_PIN U19 [get_ports {io_leds[2]}]
set_property PACKAGE_PIN V19 [get_ports {io_leds[3]}]
set_property PACKAGE_PIN W18 [get_ports {io_leds[4]}]
set_property PACKAGE_PIN U15 [get_ports {io_leds[5]}]
set_property PACKAGE_PIN U14 [get_ports {io_leds[6]}]
set_property PACKAGE_PIN V14 [get_ports {io_leds[7]}]
set_property PACKAGE_PIN V13 [get_ports {io_leds[8]}]
set_property PACKAGE_PIN V3  [get_ports {io_leds[9]}]
set_property PACKAGE_PIN W3  [get_ports {io_leds[10]}]
set_property PACKAGE_PIN U3  [get_ports {io_leds[11]}]
set_property PACKAGE_PIN P3  [get_ports {io_leds[12]}]
set_property PACKAGE_PIN N3  [get_ports {io_leds[13]}]
set_property PACKAGE_PIN P1  [get_ports {io_leds[14]}]
set_property PACKAGE_PIN L1  [get_ports {io_leds[15]}]

set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[2]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[3]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[4]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[5]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[6]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[7]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[8]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[9]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[10]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[11]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[12]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[13]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[14]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_leds[15]}]