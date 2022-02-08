analyze -file rtl_file.list
elaborate zynq_sample_design_wrapper

report_policy ALL -output ascent_lint_report.txt -verbose
