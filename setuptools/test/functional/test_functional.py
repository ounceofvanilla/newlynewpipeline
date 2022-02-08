import sys
sys.path.insert(0, 'src')
import func_class
import pytest

# Run a parametrized PyTest test
# Tests are run four times and are tagged as functional. One of them is tagged as expecting to fail
@pytest.mark.parametrize(
   'input,expected',
   [
      pytest.param(4, 5, marks=[pytest.mark.functional], id='4+1=5'),
      pytest.param(0.0, 1.0, marks=[pytest.mark.functional], id='0.0+1.0=1.0'),
      pytest.param(-1, 0, marks=[pytest.mark.functional], id='-1+1=0'),
      pytest.param(1, 5, marks=[pytest.mark.functional, pytest.mark.xfail], id='1+1=5')
   ]
)
def test_func(input, expected):
   print("Running Func Test")
   assert func_class.func(input) == expected

# Run test_func with incorrect output
# Tests are tagged with 'error' and run twice
@pytest.mark.parametrize(
   'input,error',
   [
      pytest.param('a', pytest.raises(TypeError), marks=[pytest.mark.error], id='String'),
      pytest.param(None, pytest.raises(TypeError), marks=[pytest.mark.error], id='Null'),
   ]
)
def test_input_handling(input, error):
   print("Running Input Handling Test")
   with error:
      func_class.func(input)