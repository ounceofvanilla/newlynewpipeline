Getting Started
##Explanation

The requirements packages need to be imported to our test cases. We will use Chrome browser for testing. And the assert built-in module from Node.js for assertion tests.

We also import custom elements from WebDriver such as: Builder, Key, By and until. The Key provides keys in the keyboard like: RETURN, F4, ALT etc. For a full list of WebDriver elements, you can visit selenium-webdriver docs.

require('chromedriver');
const assert = require('assert');
const {Builder, Key, By, until} = require('selenium-webdriver');
After that, we will write a simple test case for checking Google search page.

First, we need to create an instance of Chrome WebDriver.

before(async function() {
    driver = await new Builder().forBrowser('chrome').build();
});

Next, we will write steps for our test. For the element ID, you can find it by open the browser inspect feature.
describe('Checkout Google.com', function () {
    ...

    it('Search on Google', async function() {
        // Load the page
        await driver.get('https://google.com');
        // Find the search box by id
        await driver.findElement(By.id('lst-ib')).click();
        // Enter keywords and click enter
        await driver.findElement(By.id('lst-ib')).sendKeys('L3Harris stocks', Key.RETURN);
        // Wait for the results box by id
        await driver.wait(until.elementLocated(By.id('rcnt')), 10000);

        // We will get the title value and test it
        let title = await driver.getTitle();
        assert.equal(title, 'L3Harris stocks - Google Search');
    });

    // close the browser after running tests
    after(() => driver && driver.quit());
})
Running the test
This simple test can be run from the command line.

npm run test
The below result shows that the test has been successfully completed.

labuser@LNVLE3131:~/Desktop/selenium_sample$
$ mocha --recursive --timeout 10000 test.js

  Checkout Google.com
    ✓ Search on Google (2058ms)

  1 passing (3s)

Missing dependencies
I was missing some dependencies on my machine. Your machine's required dependencies may vary slightly. These were my notes during installation:

Installing Selenium

VLE image - Ubuntu 14.04

Rationale: every following command will 404 without this.
sudo apt-get update
sudo apt-get upgrade

Installs curl
sudo apt-get install curl

Installs nodejs/npm
curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
sudo apt-get install nodejs

Installs dependencies, as outlined by package.json
npm i

Updates chromedriver to the most recent version (only do this if you're having issues with chromedriver since chromedriver 2.42 is already provided in package.json)
sudo npm -g install chromedriver --unsafe-perm=true --allow-root

Installing an old version of Chrome. Only do this if you're getting Chrome version errors. Install v75.

1. Navigate to https://www.slimjet.com/chrome/google-chrome-old-version.php
2. Download your version
3. Uninstall any current instances of chrome
4, Run the .deb file you got from the website
5. Verify that it worked using sh google-chrome --version This should return 75.xxxx if it worked.