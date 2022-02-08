#-----------------------------------------------------------
# This TCL file generated with Vivado v2014.3 (64-bit)
# Sample file for Vivado FPGA builds in DOCPF
# - This project is based on Xilinx's embedded design
#   tutorial with the Zynq-7000 processor
#-----------------------------------------------------------

# Create a symlink to the workspace in case there is a space in the path
set tempWs "/home/$::env(USER)/tempWs"
file delete -force $tempWs
file link -symbolic $tempWs [pwd]

open_project $tempWs/vivado/DocpfVivadoSample.xpr
reset_project
open_bd_design $tempWs/vivado/DocpfVivadoSample.srcs/sources_1/bd/zynq_sample_design/zynq_sample_design.bd
update_compile_order -fileset sources_1
update_compile_order -fileset sim_1
save_bd_design

launch_runs synth_1

# Update VHDL files to avoid UNISIM library issues with Meridian CDC.
# This should not be necessary once Simulation capabilities are added.
# See comments in DOJ-328 for more information.
exec -ignorestderr wget -O "$tempWs/vivado/DocpfVivadoSample.srcs/sources_1/bd/zynq_sample_design/hdl/zynq_sample_design.vhd" "https://lnsvr0329.gcsd.harris.com:8443/bitbucket/projects/DEVP/repos/fpga/raw/vivado/DocpfVivadoSample.srcs/sources_1/bd/zynq_sample_design/hdl/zynq_sample_design.vhd"
exec -ignorestderr wget -O "$tempWs/vivado/DocpfVivadoSample.srcs/sources_1/bd/zynq_sample_design/hdl/zynq_sample_design_wrapper.vhd" "https://lnsvr0329.gcsd.harris.com:8443/bitbucket/projects/DEVP/repos/fpga/raw/vivado/DocpfVivadoSample.srcs/sources_1/bd/zynq_sample_design/hdl/zynq_sample_design_wrapper.vhd"

wait_on_run synth_1
open_run synth_1 -name synth_1
launch_runs impl_1 -to_step write_bitstream
wait_on_run impl_1
close_design
open_run impl_1
report_timing_summary -delay_type min_max -report_unconstrained -check_timing_verbose -max_paths 10 -input_pins -name timing_1

file mkdir $tempWs/vivado/DocpfVivadoSample.sdk
file copy -force $tempWs/vivado/DocpfVivadoSample.runs/impl_1/zynq_sample_design_wrapper.sysdef $tempWs/vivado/DocpfVivadoSample.sdk/zynq_sample_design_wrapper.hdf
