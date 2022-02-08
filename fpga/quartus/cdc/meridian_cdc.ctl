analyze ../fibonacci.vhd
elaborate

read_sdc ../SampleQuartus.sdc
analyze_intent
verify_cdc

report_policy ALL -output meridian_cdc_report.txt -verbose
