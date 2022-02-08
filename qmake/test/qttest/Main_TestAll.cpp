#include "Main_TestAll.h"

// -- setup/cleanup --
void Main_TestAll::init()
{
}

// -- tests --
void Main_TestAll::additionTest()
{
   QVERIFY2(addition.twoValues(x,y) == 9, "Addition test failed: 4 + 5 ! = 9");
   QVERIFY2(addition.twoValues(2,3) == 5, "Addition test failed: 2 + 3 ! = 5");
}

void Main_TestAll::multiplyTest()
{
   QVERIFY2(multiply.twoValues(x,y) == 20, "Multipication test failed: 4 x 5 ! = 20");
   QVERIFY2(multiply.twoValues(2,3) == 6, "Multipication test failed: 2 x 3 ! = 6");
}

// generate basic main: no GUI, no events
QTEST_APPLESS_MAIN(Main_TestAll)