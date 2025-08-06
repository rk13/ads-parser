package ss.parser.ad.flat;

import org.w3c.dom.Element;
import ss.parser.ad.Ad;
import ss.parser.ad.AdConfigImpl;

import java.util.regex.Matcher;

abstract class FlatConfig extends AdConfigImpl {
    @Override
    public Ad newAd(Element element, Matcher matcher) {
        int price = parseInt(matcher.group("price"));
        int area = parseInt(matcher.group("area"));
        int ppm2 = (area != 0) ? price / area : 0;
        return new Flat(element,
                "daugavpils",
                parseString(matcher.group("address")),
                parseString(matcher.group("series")),
                parseInt(matcher.group("rooms")),
                parseInt(matcher.group("area")),
                parseInt(matcher.group("floor")),
                parseInt(matcher.group("floors")),
                price,
                ppm2);
    }
}
