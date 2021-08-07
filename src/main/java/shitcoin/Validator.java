package shitcoin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Validator {

    private WebDriver driver;
    private final int waitTime = 15; // seconds

    public boolean isEmptyLogo(Element link) {
        return link.child(0).attr("src").equals("/images/main/empty-token.png");
    }

    public boolean isEnoughHolders(String tokenId) throws IOException {
        Document doc = Jsoup.connect(Common.BSC_TOKEN_URL + tokenId).get();
        Element holder = doc.select("div:containsOwn(Holders:)").get(0);
        String numberOfHoldersTxt = holder.parent().child(1).child(0).child(0).text();
        // convert "1,000 addresses" -> 1000
        int numberOfHolders = Integer.parseInt(numberOfHoldersTxt.replace(" addresses", "").replace(",", ""));
        if (numberOfHolders >= Common.MIN_HOLDERS && numberOfHolders <= Common.MAX_HOLDERS) {
            return true;
        }
        return false;
    }

    public boolean isEnoughLPAndVolume(String tokenId) {
        boolean result = false;
        try {
            startDriver(tokenId);
            result = isEnoughLP(tokenId) && isEnough24hVolume(tokenId) ;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something wrong with: " + tokenId);
        } finally {
            driver.quit();
        }

//        return (isEnough24hVolume());
        return result;
    }

    public boolean isEnoughLP(String tokenId)
            throws IOException, URISyntaxException, InterruptedException, ScriptException {
        // lp = liquidity pool
        WebDriverWait wait = new WebDriverWait(driver, waitTime);
        
        // click anywhere to remove screen overlay
        By overlayLocator = By.xpath("//div[contains(@class, 'top-0 fixed z-20 w-full left-0 h-full bg-gray-900 bg-opacity-60')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(overlayLocator));
        this.driver.findElement(overlayLocator).click();

        // find LP in BNB
        By liqInfolocator = By.xpath("//*[text() = 'Liquidity Info']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(liqInfolocator));
        this.driver.findElement(liqInfolocator).click();
        
        // scroll to bottom to see BNB value
        driver.findElement(By.cssSelector("body")).sendKeys(Keys.CONTROL, Keys.END);
        try {
            By bnbPoolLocator = By.xpath("//*[starts-with (text(), 'BNB pooled')]");
            wait.until(ExpectedConditions.visibilityOfElementLocated(bnbPoolLocator));
            Thread.sleep(8000); // sleep 2s to wait for pancakeswap v1, v2 bnb pool
            List<WebElement> bnbPoolLocators = this.driver.findElements(bnbPoolLocator);
            String amountOfBnbTxt = "";
            if (bnbPoolLocators.size() == 2) { // avoid bnb pool in pancake swap v1
                System.out.println("Pancakeswap v2: " + bnbPoolLocators.get(0).getText());
                System.out.println("Pancakeswap v1: " + bnbPoolLocators.get(1).getText());
                amountOfBnbTxt = bnbPoolLocators.get(0).getText().replace("BNB pooled: ", "");
            } else if (bnbPoolLocators.size() == 1) {
                amountOfBnbTxt = this.driver.findElement(bnbPoolLocator).getText().replace("BNB pooled: ", "");
            } else {
                System.out.println("Something wrong with bnb pool: " + tokenId);
            }
            
            // replace 12,000.30 -> 12000.30
            amountOfBnbTxt = amountOfBnbTxt.replace(",","");
            double amountOfBnb = Double.parseDouble(amountOfBnbTxt);
            System.out.println("Amount of bnb: " + amountOfBnb);
            if (amountOfBnb >= Common.MIN_BNB) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("BNB pooled not found!!! " + tokenId);
        }
        return false;

    }

    public boolean isEnough24hVolume(String tokenId) {
        WebDriverWait wait = new WebDriverWait(driver, waitTime);
        try {
            // find 24h volume
            By volumeLabelLocator = By.xpath("//*[text() = '24hr Volume']");
            wait.until(ExpectedConditions.visibilityOfElementLocated(volumeLabelLocator));
            WebElement parent = this.driver.findElement(volumeLabelLocator).findElement(By.xpath("./..")); // get parent
                                                                                                           // div
            By volumeValueLocator = By.xpath("./h4"); // get h4 contains 24h volume
            wait.until((ExpectedCondition<Boolean>) driver -> parent.findElement(volumeValueLocator).getText()
                    .length() != 0);

            // convert "1,000$" -> 1000
            String amountOfVolumeTxt = parent.findElement(volumeValueLocator).getText().replace("$", "").replace(",",
                    "");
            double amountOfVolume = Double.parseDouble(amountOfVolumeTxt);
            System.out.printf("Amount of 24h volume: %,.0f", amountOfVolume);
            System.out.println();
            if (amountOfVolume >= Common.MIN_24H_VOLUME) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("24h volume not found!!! " + tokenId);
        }

        return false;
    }

    private void startDriver(String tokenId) {
        // Set the path of chrome driver
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\NamNVH\\Desktop\\ShitcoinFinder\\chromedriver.exe");
        // disable print log to console
        System.setProperty("webdriver.chrome.silentOutput", "true");
        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        // Create object of ChromeOptions class
        ChromeOptions options = new ChromeOptions();
//        options.setBinary("C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe");

        this.driver = new ChromeDriver(options);
        this.driver.get(Common.BOGGED_FINANCE_TOKEN_URL + tokenId);
    }

}
