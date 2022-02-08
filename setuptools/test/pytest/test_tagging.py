#++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
# WARNING: EXPORT CONTROLLED - ITAR
# These item(s) / technical data are controlled by the
# Department of State, International Traffic in Arms
# Regulations (ITAR), 22 CFR parts 120-130, and cannot be exported
# from the United States or shared with a foreign person
# without prior approval from the U.S. Department of State.
#++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
# +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
# ++ Unlimited Rights
# ++ WARNING: Do Not Use On A Privately Funded Program Without Permission.
# +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#
# ** Copyright (C) H/C 2018 - All Rights Reserved
#
# *****************************************************************************
# FILE Name:        test_embedded.py
#
# CLASSIFICATION:   Unclassified
#
# DESCRIPTION:      Example of using pytest with tagging
#
# REQUIREMENTS:     N/A
#
# LIMITATIONS:      N/A
#
# CHANGE HISTORY:  Change History is maintained in BitBucket log for this file. 
# *****************************************************************************
import sys
sys.path.insert(0, 'src')
import func_class
import pytest

# *****************************************************************************
# DEF NAME:         test_answer_smoke
#
# CLASSIFICATION:   Unclassified
#
# DESCRIPTION:      This is a sample test.
#
# INPUTS:           number
#
# OUTPUTS:          number
#
# LIMITATIONS:      N/A
#
# TAGS:             @smoke
#                  
# *****************************************************************************
@pytest.mark.smoke
def test_tag_smoke():
	print("Running Smoke Test")
	assert func_class.func(3) == 4

# *****************************************************************************
# DEF NAME:         test_answer_nightly
#
# CLASSIFICATION:   Unclassified
#
# DESCRIPTION:      This is a sample test.
#
# INPUTS:           number
#
# OUTPUTS:          number
#
# LIMITATIONS:      N/A
#
# TAGS:             @nightly
#                  
# *****************************************************************************
@pytest.mark.nightly
def test_tag_nightly():
	print("Running Nightly Test")
	assert func_class.func(4) == 5

# *****************************************************************************
# DEF NAME:         test_answer_weekend
#
# CLASSIFICATION:   Unclassified
#
# DESCRIPTION:      This is a sample test.
#
# INPUTS:           number
#
# OUTPUTS:          number
#
# LIMITATIONS:      N/A
#
# TAGS:             @weekend
#                  
# *****************************************************************************
@pytest.mark.weekend
def test_tag_weekend():
	print("Running Weekend Test")
	assert func_class.func(5) == 6

# *****************************************************************************
# DEF NAME:         test_answer_weekend
#
# CLASSIFICATION:   Unclassified
#
# DESCRIPTION:      This is a sample test that always fails.
#
# INPUTS:           number
#
# OUTPUTS:          number
#
# LIMITATIONS:      N/A
#
# TAGS:             @fail
#
# *****************************************************************************
@pytest.mark.fail
def test_answer_fail():
	print("Running Fail Test")
	print("Note: This test is designed to fail for pipeline testing")
	assert func_class.func(5) == 7


def main():
  print("Hello World!")