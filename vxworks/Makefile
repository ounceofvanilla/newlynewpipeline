# Wind River Workbench generated Makefile.
# Do not edit!!!
#
# The file ".wrmakefile" is the template used by the Wind River Workbench to
# generate the makefiles of this project. Add user-specific build targets and
# make rules only(!) in this project's ".wrmakefile" file. These will then be
# automatically dumped into the makefiles.

WIND_HOME := $(subst \,/,$(WIND_HOME))
WIND_BASE := $(subst \,/,$(WIND_BASE))
WIND_USR := $(subst \,/,$(WIND_USR))
WRVX_COMPBASE := $(subst \,/,$(WRVX_COMPBASE))

all : pre_build main_all post_build

_clean ::
	@echo "make: removing targets and objects of `pwd`"

TRACE=0
TRACEON=$(TRACE:0=@)
TRACE_FLAG=$(TRACEON:1=)

JOBS?=1
TARGET_JOBS?=$(JOBS)

MAKEFILE := Makefile

FLEXIBLE_BUILD := 1

DEBUG_MODE = 1
ifeq ($(DEBUG_MODE),1)
MODE_DIR := Debug
else
MODE_DIR := NonDebug
endif
OBJ_DIR := .
PRJ_ROOT_DIR := .



#Global Build Macros
PROJECT_TYPE = DKM
DEFINES = 
EXPAND_DBG = 0


#BuildSpec specific Build Macros
VX_CPU_FAMILY = simpc
CPU = SIMNT
TOOL_FAMILY = gnu
TOOL = gnu
TOOL_PATH = 
CC_ARCH_SPEC = -mtune=i486 -march=i486
VSB_DIR = $(WIND_BASE)/target/lib
VSB_CONFIG_FILE = $(VSB_DIR)/h/config/vsbConfig.h
LIBPATH = 
LIBS = 

IDE_INCLUDES = -I$(WIND_BASE)/target/h -I$(WIND_BASE)/target/h/wrn/coreip 

IDE_LIBRARIES = 

IDE_DEFINES = -DCPU=_VX_$(CPU) -DTOOL_FAMILY=$(TOOL_FAMILY) -DTOOL=$(TOOL) -D_WRS_KERNEL -D_VSB_CONFIG_FILE=\"$(VSB_DIR)/h/config/vsbConfig.h\"   -DIP_PORT_VXWORKS=69 



#BuildTool flags
ifeq ($(DEBUG_MODE),1)
DEBUGFLAGS_C-Compiler = -g
DEBUGFLAGS_C++-Compiler = -g
DEBUGFLAGS_Linker = -g
DEBUGFLAGS_Partial-Image-Linker = 
DEBUGFLAGS_Librarian = 
DEBUGFLAGS_Assembler = -g
else
DEBUGFLAGS_C-Compiler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
DEBUGFLAGS_C++-Compiler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
DEBUGFLAGS_Linker =  -O2 -nostdlib -fno-builtin -fno-defer-pop
DEBUGFLAGS_Partial-Image-Linker = 
DEBUGFLAGS_Librarian = 
DEBUGFLAGS_Assembler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
endif


#Project Targets
PROJECT_TARGETS = HelloWorld/$(MODE_DIR)/HelloWorld.out \
	 HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage.o


#Rules

# HelloWorld
ifeq ($(DEBUG_MODE),1)
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_C-Compiler = -g
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_C++-Compiler = -g
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Linker = -g
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Partial-Image-Linker = 
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Librarian = 
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Assembler = -g
else
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_C-Compiler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_C++-Compiler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Linker =  -O2 -nostdlib -fno-builtin -fno-defer-pop
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Partial-Image-Linker = 
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Librarian = 
HelloWorld/$(MODE_DIR)/% : DEBUGFLAGS_Assembler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
endif
HelloWorld/$(MODE_DIR)/% : IDE_INCLUDES = -I$(WIND_BASE)/target/h -I$(WIND_BASE)/target/h/wrn/coreip 
HelloWorld/$(MODE_DIR)/% : IDE_LIBRARIES = 
HelloWorld/$(MODE_DIR)/% : IDE_DEFINES = -DCPU=_VX_$(CPU) -DTOOL_FAMILY=$(TOOL_FAMILY) -DTOOL=$(TOOL) -D_WRS_KERNEL -D_VSB_CONFIG_FILE=\"$(VSB_DIR)/h/config/vsbConfig.h\"   -DIP_PORT_VXWORKS=69 
HelloWorld/$(MODE_DIR)/% : PROJECT_TYPE = DKM
HelloWorld/$(MODE_DIR)/% : DEFINES = 
HelloWorld/$(MODE_DIR)/% : EXPAND_DBG = 0
HelloWorld/$(MODE_DIR)/% : VX_CPU_FAMILY = simpc
HelloWorld/$(MODE_DIR)/% : CPU = SIMNT
HelloWorld/$(MODE_DIR)/% : TOOL_FAMILY = gnu
HelloWorld/$(MODE_DIR)/% : TOOL = gnu
HelloWorld/$(MODE_DIR)/% : TOOL_PATH = 
HelloWorld/$(MODE_DIR)/% : CC_ARCH_SPEC = -mtune=i486 -march=i486
HelloWorld/$(MODE_DIR)/% : VSB_DIR = $(WIND_BASE)/target/lib
HelloWorld/$(MODE_DIR)/% : VSB_CONFIG_FILE = $(VSB_DIR)/h/config/vsbConfig.h
HelloWorld/$(MODE_DIR)/% : LIBPATH = 
HelloWorld/$(MODE_DIR)/% : LIBS = 
HelloWorld/$(MODE_DIR)/% : OBJ_DIR := HelloWorld/$(MODE_DIR)

OBJECTS_HelloWorld = HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage.o

ifeq ($(TARGET_JOBS),1)
HelloWorld/$(MODE_DIR)/HelloWorld.out : $(OBJECTS_HelloWorld)
	$(TRACE_FLAG)if [ ! -d "`dirname "$@"`" ]; then mkdir -p "`dirname "$@"`"; fi;echo "building $@";rm -f "$@";nmpentium $(OBJECTS_HelloWorld) | tclsh $(WIND_BASE)/host/resource/hutils/tcl/munch.tcl -c pentium -tags $(VSB_DIR)/tags/simpc/SIMNT/common/dkm.tags > $(OBJ_DIR)/ctdt.c; $(TOOL_PATH)ccpentium $(DEBUGFLAGS_Linker) $(CC_ARCH_SPEC) -fdollars-in-identifiers -Wall   $(ADDED_CFLAGS) $(IDE_INCLUDES) $(ADDED_INCLUDES)  $(IDE_DEFINES) $(DEFINES) -o $(OBJ_DIR)/ctdt.o -c $(OBJ_DIR)/ctdt.c; $(TOOL_PATH)ccpentium -r -nostdlib -Wl,-X -T $(WIND_BASE)/target/h/tool/gnu/ldscripts/link.OUT -o "$@" $(OBJ_DIR)/ctdt.o $(OBJECTS_HelloWorld) $(IDE_LIBRARIES) $(LIBPATH) $(LIBS) $(ADDED_LIBPATH) $(ADDED_LIBS) && if [ "$(EXPAND_DBG)" = "1" ]; then plink "$@";fi

else
HelloWorld/$(MODE_DIR)/HelloWorld.out : HelloWorld/$(MODE_DIR)/HelloWorld.out_jobs

endif
HelloWorld/$(MODE_DIR)/HelloWorld_compile_file : $(FILE) ;

_clean :: HelloWorld/$(MODE_DIR)/HelloWorld_clean

HelloWorld/$(MODE_DIR)/HelloWorld_clean : 
	$(TRACE_FLAG)if [ -d "HelloWorld" ]; then cd "HelloWorld"; rm -rf $(MODE_DIR); fi


# HelloWorld_partialImage
ifeq ($(DEBUG_MODE),1)
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_C-Compiler = -g
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_C++-Compiler = -g
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Linker = -g
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Partial-Image-Linker = 
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Librarian = 
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Assembler = -g
else
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_C-Compiler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_C++-Compiler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Linker =  -O2 -nostdlib -fno-builtin -fno-defer-pop
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Partial-Image-Linker = 
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Librarian = 
HelloWorld_partialImage/$(MODE_DIR)/% : DEBUGFLAGS_Assembler =  -O2 -nostdlib -fno-builtin -fno-defer-pop
endif
HelloWorld_partialImage/$(MODE_DIR)/% : IDE_INCLUDES = -I$(WIND_BASE)/target/h -I$(WIND_BASE)/target/h/wrn/coreip 
HelloWorld_partialImage/$(MODE_DIR)/% : IDE_LIBRARIES = 
HelloWorld_partialImage/$(MODE_DIR)/% : IDE_DEFINES = -DCPU=_VX_$(CPU) -DTOOL_FAMILY=$(TOOL_FAMILY) -DTOOL=$(TOOL) -D_WRS_KERNEL -D_VSB_CONFIG_FILE=\"$(VSB_DIR)/h/config/vsbConfig.h\"   -DIP_PORT_VXWORKS=69 
HelloWorld_partialImage/$(MODE_DIR)/% : PROJECT_TYPE = DKM
HelloWorld_partialImage/$(MODE_DIR)/% : DEFINES = 
HelloWorld_partialImage/$(MODE_DIR)/% : EXPAND_DBG = 0
HelloWorld_partialImage/$(MODE_DIR)/% : VX_CPU_FAMILY = simpc
HelloWorld_partialImage/$(MODE_DIR)/% : CPU = SIMNT
HelloWorld_partialImage/$(MODE_DIR)/% : TOOL_FAMILY = gnu
HelloWorld_partialImage/$(MODE_DIR)/% : TOOL = gnu
HelloWorld_partialImage/$(MODE_DIR)/% : TOOL_PATH = 
HelloWorld_partialImage/$(MODE_DIR)/% : CC_ARCH_SPEC = -mtune=i486 -march=i486
HelloWorld_partialImage/$(MODE_DIR)/% : VSB_DIR = $(WIND_BASE)/target/lib
HelloWorld_partialImage/$(MODE_DIR)/% : VSB_CONFIG_FILE = $(VSB_DIR)/h/config/vsbConfig.h
HelloWorld_partialImage/$(MODE_DIR)/% : LIBPATH = 
HelloWorld_partialImage/$(MODE_DIR)/% : LIBS = 
HelloWorld_partialImage/$(MODE_DIR)/% : OBJ_DIR := HelloWorld_partialImage/$(MODE_DIR)

HelloWorld_partialImage/$(MODE_DIR)/Objects/HelloWorld/helloworld.o : $(PRJ_ROOT_DIR)/helloworld.c $(FORCE_FILE_BUILD)
	$(TRACE_FLAG)if [ ! -d "`dirname "$@"`" ]; then mkdir -p "`dirname "$@"`"; fi;echo "building $@"; $(TOOL_PATH)ccpentium $(DEBUGFLAGS_C-Compiler) $(CC_ARCH_SPEC) -ansi -fno-zero-initialized-in-bss  -Wall    -MD -MP $(IDE_DEFINES) $(DEFINES) $(ADDED_CFLAGS) $(IDE_INCLUDES) $(ADDED_INCLUDES) -o "$@" -c "$<"


OBJECTS_HelloWorld_partialImage = HelloWorld_partialImage/$(MODE_DIR)/Objects/HelloWorld/helloworld.o

ifeq ($(TARGET_JOBS),1)
HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage.o : $(OBJECTS_HelloWorld_partialImage)
	$(TRACE_FLAG)if [ ! -d "`dirname "$@"`" ]; then mkdir -p "`dirname "$@"`"; fi;echo "building $@"; $(TOOL_PATH)ccpentium -r -nostdlib -Wl,-X  -o "$@" $(OBJECTS_HelloWorld_partialImage) $(ADDED_OBJECTS) $(IDE_LIBRARIES) $(LIBPATH) $(LIBS) $(ADDED_LIBPATH) $(ADDED_LIBS) && if [ "$(EXPAND_DBG)" = "1" ]; then plink "$@";fi

else
HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage.o : HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage.o_jobs

endif
HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage_compile_file : $(FILE) ;

_clean :: HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage_clean

HelloWorld_partialImage/$(MODE_DIR)/HelloWorld_partialImage_clean : 
	$(TRACE_FLAG)if [ -d "HelloWorld_partialImage" ]; then cd "HelloWorld_partialImage"; rm -rf $(MODE_DIR); fi

force : 

TARGET_JOBS_RULE?=echo "Update the makefile template via File > Import > Build Settings : Update makefile template";exit 1
%_jobs : 
	$(TRACE_FLAG)$(TARGET_JOBS_RULE)

DEP_FILES := HelloWorld_partialImage/$(MODE_DIR)/Objects/HelloWorld/helloworld.d
-include $(DEP_FILES)


WIND_SCOPETOOLS_BASE := $(subst \,/,$(WIND_SCOPETOOLS_BASE))

clean_scopetools :
	$(TRACE_FLAG)rm -rf $(PRJ_ROOT_DIR)/.coveragescope/db

CLEAN_STEP := clean_scopetools


-include $(PRJ_ROOT_DIR)/*.makefile

-include *.makefile

TARGET_JOBS_RULE=$(MAKE) -f $(MAKEFILE) --jobs $(TARGET_JOBS) $(MFLAGS) $* TARGET_JOBS=1
ifeq ($(JOBS),1)
main_all : external_build  $(PROJECT_TARGETS)
	@echo "make: built targets of `pwd`"
else
main_all : external_build 
	@$(MAKE) -f $(MAKEFILE) --jobs $(JOBS) $(MFLAGS) $(PROJECT_TARGETS) TARGET_JOBS=1 &&\
	echo "make: built targets of `pwd`"
endif 

# entry point for extending the build
external_build ::
	@echo ""

# main entry point for pre processing prior to the build
pre_build :: $(PRE_BUILD_STEP) generate_sources
	@echo ""

# entry point for generating sources prior to the build
generate_sources ::
	@echo ""

# main entry point for post processing after the build
post_build :: $(POST_BUILD_STEP) deploy_output
	@echo ""

# entry point for deploying output after the build
deploy_output ::
	@echo ""

clean :: external_clean $(CLEAN_STEP) _clean

# entry point for extending the build clean
external_clean ::
	@echo ""