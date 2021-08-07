package shitcoin;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 0; i < Common.RUN_TIMES; i++) {
            getTokenIds();
            Thread.sleep(3000);
        }
        
        System.out.println("Finished!!!");
    }

    private static List<String> getTokenIds() {
        Validator validator = new Validator();
        List<String> tokenIds = new ArrayList<String>();
        try {
            Document doc = Jsoup.connect(Common.BSC_BEP20_URL).get();
            Elements links = doc.select("[href*=/token/]");
            for (Element link : links) {
                // check for empty-token-logo in src of <img>
                if (validator.isEmptyLogo(link)) {
                    String tokenId = link.attr("href").replace("/token/", "");
                    tokenIds.add(tokenId);
                }
            }

            // remove duplicated contract id
            tokenIds = new ArrayList<String>(new HashSet<String>(tokenIds));
//            validator.isEnoughLPAndVolume(tokenIds.get(0)); // use to test isEnoughLP() method

            for (String tokenId : tokenIds) {
//                System.out.println(tokenId);
                if (validator.isEnoughHolders(tokenId) && validator.isEnoughLPAndVolume(tokenId)) {
                    openURLs(tokenId);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokenIds;
    }

    private static void openURLs(String tokenId) throws IOException, InterruptedException {
        Desktop.getDesktop().browse(URI.create(Common.BSC_TOKEN_URL + tokenId + "#balances"));
        Desktop.getDesktop().browse(URI.create(Common.BOGGED_FINANCE_TOKEN_URL + tokenId));
        Desktop.getDesktop().browse(URI.create(Common.TOKEN_SNIFFER_URL + tokenId));
    }

}
