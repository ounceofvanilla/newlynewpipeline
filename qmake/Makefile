CXX = c++
PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

SRC_DIR = ./src
TEST_DIR = ./test/src

all:
	cd $(SRC_DIR) && $(MAKE)
	cd $(TEST_DIR) && $(MAKE)

clean:
	cd $(SRC_DIR) && $(MAKE) clean
	cd $(TEST_DIR) && $(MAKE) clean
