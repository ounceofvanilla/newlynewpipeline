using System;
using System.IO;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace HelloWorldUnitTest
{
    [TestClass]
    public class HelloWorldTest
    {
        [TestMethod]
        public void Test_RunMain()
        {
            // Arrange
            string expected = "Hello World!";

            // Act

            // Represents a multable string of characters.
            StringBuilder sb = new StringBuilder();

            // Implements a TextWriter for writing information to a string.
            StringWriter sw = new StringWriter(sb);

            // Set the Console.out to a specified TextWriter object
            Console.SetOut(sw);

            HelloWorld.MyClass.greet();

            // Assert
            Assert.AreEqual(expected.ToString(), sw.ToString().Trim());
        }

        [TestMethod]
        public void Test_RunGreet()
        {
            // Arrange
            string expected = "Hello World!";

            // Act

            // Represents a multable string of characters.
            StringBuilder sb = new StringBuilder();

            // Implements a TextWriter for writing information to a string.
            StringWriter sw = new StringWriter(sb);

            // Set the Console.out to a specified TextWriter object
            Console.SetOut(sw);

            HelloWorld.MyClass.greet();

            // Assert
            Assert.AreEqual(expected.ToString(), sw.ToString().Trim());
        }
    }
}