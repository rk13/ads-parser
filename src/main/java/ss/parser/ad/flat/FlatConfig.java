package ss.parser.ad.flat;

import org.w3c.dom.Element;
import ss.parser.ad.Ad;
import ss.parser.ad.AdConfigImpl;

import java.util.regex.Matcher;

abstract class FlatConfig extends AdConfigImpl {
    @Override
    public Ad newAd(Element element, Matcher matcher) {
        return new Flat(element,
                parseString(matcher.group("region")),
                parseString(matcher.group("address")),
                parseString("0"),//matcher.group("series")),
                parseInt("0"),//matcher.group("rooms")),
                parseInt("0"),//matcher.group("area")),
                parseInt("0"),//matcher.group("floor")),
                parseInt("0"),//matcher.group("floors")),
                parseInt(matcher.group("price")),
                parseInt("0"));//matcher.group("ppm2")));
    }
}
