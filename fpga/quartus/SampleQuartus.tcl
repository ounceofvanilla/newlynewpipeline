# Copyright (C) 2017  Intel Corporation. All rights reserved.
# Your use of Intel Corporation's design tools, logic functions 
# and other software and tools, and its AMPP partner logic 
# functions, and any output files from any of the foregoing 
# (including device programming or simulation files), and any 
# associated documentation or information are expressly subject 
# to the terms and conditions of the Intel Program License 
# Subscription Agreement, the Intel Quartus Prime License Agreement,
# the Intel FPGA IP License Agreement, or other applicable license
# agreement, including, without limitation, that your use is for
# the sole purpose of programming logic devices manufactured by
# Intel and sold by Intel or its authorized distributors.  Please
# refer to the applicable agreement for further details.

# Quartus Prime: Generate Tcl File for Project
# File: SampleQuartus2.tcl
# Generated on: Mon Jun  1 03:13:11 2020

# Load Quartus Prime Tcl Project package
package require ::quartus::project

# Load Quartus Prime Tcl Flow package
package require ::quartus::flow

set need_to_close_project 0
set make_assignments 1

# Check that the right project is open
if {[is_project_open]} {
	if {[string compare $quartus(project) "SampleQuartus"]} {
		puts "Project SampleQuartus is not open"
		set make_assignments 0
	}
} else {
	# Only open if not already open
	if {[project_exists SampleQuartus]} {
		project_open -revision SampleQuartus SampleQuartus
	} else {
		project_new -revision SampleQuartus SampleQuartus
	}
	set need_to_close_project 1
}

# Make assignments
if {$make_assignments} {
	set_global_assignment -name FAMILY "Cyclone 10 GX"
	set_global_assignment -name DEVICE 10CX220YF780I5G
	set_global_assignment -name ORIGINAL_QUARTUS_VERSION 17.1.0
	set_global_assignment -name PROJECT_CREATION_TIME_DATE "16:55:46  MAY 27, 2020"
	set_global_assignment -name LAST_QUARTUS_VERSION "17.1.0 Pro Edition"
	set_global_assignment -name PROJECT_OUTPUT_DIRECTORY output_files
	set_global_assignment -name ERROR_CHECK_FREQUENCY_DIVISOR 4
	set_global_assignment -name MIN_CORE_JUNCTION_TEMP "-40"
	set_global_assignment -name MAX_CORE_JUNCTION_TEMP 100
	set_global_assignment -name POWER_AUTO_COMPUTE_TJ ON
	set_global_assignment -name POWER_PRESET_COOLING_SOLUTION "23 MM HEAT SINK WITH 200 LFPM AIRFLOW"
	set_global_assignment -name POWER_BOARD_THERMAL_MODEL "NONE (CONSERVATIVE)"
	set_global_assignment -name VHDL_FILE fibonacci.vhd
	set_global_assignment -name ENABLE_CONFIGURATION_PINS OFF
	set_global_assignment -name ENABLE_BOOT_SEL_PIN OFF
	set_global_assignment -name STRATIXV_CONFIGURATION_SCHEME "PASSIVE SERIAL"
	set_global_assignment -name GENERATE_PR_RBF_FILE OFF
	set_global_assignment -name CRC_ERROR_OPEN_DRAIN ON
	set_global_assignment -name RESERVE_ALL_UNUSED_PINS_WEAK_PULLUP "AS INPUT TRI-STATED WITH WEAK PULL-UP"
	set_global_assignment -name ACTIVE_SERIAL_CLOCK FREQ_100MHZ
	set_global_assignment -name FLOW_DISABLE_ASSEMBLER OFF
	set_global_assignment -name SDC_FILE SampleQuartus.sdc
	set_global_assignment -name USE_CONFIGURATION_DEVICE OFF
	set_location_assignment PIN_AA7 -to clk
	set_location_assignment PIN_U4 -to done
	set_location_assignment PIN_AB5 -to go
	set_location_assignment PIN_Y7 -to n[7]
	set_location_assignment PIN_AE4 -to n[6]
	set_location_assignment PIN_AH3 -to n[5]
	set_location_assignment PIN_Y6 -to n[4]
	set_location_assignment PIN_AE1 -to n[3]
	set_location_assignment PIN_AA1 -to n[2]
	set_location_assignment PIN_AF2 -to n[1]
	set_location_assignment PIN_Y5 -to n[0]
	set_location_assignment PIN_L4 -to result[7]
	set_location_assignment PIN_V2 -to result[6]
	set_location_assignment PIN_J2 -to result[5]
	set_location_assignment PIN_J3 -to result[4]
	set_location_assignment PIN_U3 -to result[3]
	set_location_assignment PIN_M4 -to result[2]
	set_location_assignment PIN_G1 -to result[1]
	set_location_assignment PIN_T9 -to result[0]
	set_location_assignment PIN_AD2 -to rst

	# Commit assignments
	export_assignments

	# Compile Project
	execute_flow -compile

	# Close project
	if {$need_to_close_project} {
		project_close
	}
}