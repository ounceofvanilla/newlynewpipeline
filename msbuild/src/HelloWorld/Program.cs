using System;

namespace HelloWorld
{
    public class MyClass
    {
        public static MyClass Create()
        {
            if (flag) return null;
            return new MyClass();
        }

        public static bool flag;

        public void foo()
        {
            //Do something
        }
        public static void greet()
        {
            Console.WriteLine("Hello World!");
        }

        public static void Main(string[] args)
        {
            greet();
        }

    }

    public class MyOtherClass
    {
        public void foo()
        {
            MyClass obj = MyClass.Create();
            //obj is never verified for null values. Used for testing Static Code Analysis
            obj.foo();
            //obj2 is intentionally never used. Used for testing Static Code Analysis
            MyClass obj2 = MyClass.Create();
            //if loop with unreachable code. Used for testing Static Code Analysis
            if (false)
            {
                Console.WriteLine("Unreachable Code");
            }
        }
    }
}