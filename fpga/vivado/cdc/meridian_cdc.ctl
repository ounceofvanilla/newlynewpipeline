analyze -file rtl_file.list
elaborate zynq_sample_design_wrapper

analyze_intent
verify_cdc

report_policy ALL -output meridian_cdc_report.txt -verbose
