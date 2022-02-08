import unittest
import sys
sys.path.insert(0, 'src')
import func_class
import xmlrunner

class TestStringMethods(unittest.TestCase):

    def test_upper(self):
        result = func_class.func(4)
        self.assertEqual(result, 5)

if __name__ == '__main__':
    with open('test_results.xml', 'wb') as output:
        unittest.main(
            testRunner=xmlrunner.XMLTestRunner(output=output),
            failfast=False, buffer=False, catchbreak=False)