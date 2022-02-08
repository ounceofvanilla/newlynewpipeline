analyze ../fibonacci.vhd
elaborate

report_policy ALL -output ascent_lint_report.txt -verbose
