#pragma once

#include "Addition.h"
#include "Multiply.h"

#include <QtTest>

class Main_TestAll: public QObject
{
   Q_OBJECT

private slots:
   // -- setup/cleanup --
   void init();

   // -- tests --
   void additionTest();
   void multiplyTest();

private:
   const int x = 4;
   const int y = 5;
   Addition addition;
   Multiply multiply;
};