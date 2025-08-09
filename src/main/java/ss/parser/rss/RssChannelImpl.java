package ss.parser.rss;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ss.parser.ad.Ad;
import ss.parser.ad.AdConfig;
import ss.parser.mail.MailService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class RssChannelImpl extends RssElementImpl implements RssChannel {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final AdConfig adConfig;
    private final MailService mailService;
    private final Pattern pattern;
    private final ZonedDateTime lastBuildDate;
    private final int ttl;
    private final List<Ad> ads;

    public RssChannelImpl(AdConfig adConfig, MailService mailService) {
        super(newElement(adConfig.getUrl(), adConfig.getTimeout()));
        this.adConfig = adConfig;
        this.mailService = mailService;
        String tmpRegex = 
            "Iela: <b>(?<address>.+?)</b>\\s*<br\\s*/?>\\s*" +
            "Ist\\.: <b>(?<rooms>\\d+)</b>\\s*<br\\s*/?>\\s*" +
            "m\\S+: <b>(?<area>\\d+)</b>\\s*<br\\s*/?>\\s*" +
            "Stāvs: <b>(?<floor>\\d+)\\s*/\\s*(?<floors>\\d+)</b>\\s*<br\\s*/?>\\s*" +
            "Sērija: <b>(?<series>.+?)</b>\\s*<br\\s*/?>\\s*" +
            ":\\s*<b>[\\d.,]+\\s*€</b>\\s*<br\\s*/?>\\s*" +
            "Cena: <b>(?<price>[\\d.,]+)\\s*€</b>";
        pattern = Pattern.compile(tmpRegex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        lastBuildDate = parseDate(getContent("lastBuildDate"));
        ttl = Integer.parseInt(getContent("ttl"));
        ads = parseAds();
    }

    private static Element newElement(URL url, Duration timeout) {
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(Math.toIntExact(timeout.toMillis()));
            con.setReadTimeout(Math.toIntExact(timeout.toMillis()));
            con.setRequestProperty("User-Agent", getHttpAgent());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(con.getInputStream());
            Element rss = doc.getDocumentElement();

            return (Element) rss.getElementsByTagName("channel").item(0);

        } catch (Exception e) {
            throw new RuntimeException("Can not open RSS channel " + url, e);
        }
    }

    private static String getHttpAgent() {
        return System.getProperty("http.agent",
                System.getenv().getOrDefault("HTTP_AGENT",
                        "Java/" + System.getProperty("java.version")));
    }

    private List<Ad> parseAds() {
        String tmpReplace = "Адрес: <b>${address}</b><br/>" +
        "Комнат: <b>${rooms}</b><br/>" +
        "Площадь: <b>${area} м2</b><br/>" +
        "Этаж: <b>${floor}/${floors}</b><br/>" +
        "Серия: <b>${series}</b><br/>" +
        "Цена: <b>${price} €</b>";
        NodeList nodeList = getElement().getElementsByTagName("item");
        List<Ad> ads = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element item = (Element) nodeList.item(i);
            Node node = item.getElementsByTagName("description").item(0);
            String description = node.getTextContent();
            Matcher matcher = pattern.matcher(description);
            log.info("{}", matcher);
            log.info("{}", tmpReplace);
            if (matcher.find()) {
                node.setTextContent(pattern.matcher(description).replaceAll(tmpReplace));
                ads.add(adConfig.newAd(item, matcher));
            } else {
                String message = "RSS item description does not match regex:\r\n" + description;
                log.error(message);
                mailService.sendError(getClass().getName(), message);
            }
        }
        return Collections.unmodifiableList(ads);
    }

    @Override
    public String toString() {
        return "RSS channel " + getLink();
    }
}
