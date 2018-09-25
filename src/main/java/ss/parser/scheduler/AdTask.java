package ss.parser.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ss.parser.mail.MailService;
import ss.parser.rss.Ad;
import ss.parser.rss.Channel;
import ss.parser.rss.ChannelImpl;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AdTask implements SchedulerTask {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final AdConfig adConfig;
    private final MailService mailService;
    private ZonedDateTime lastBuildDate = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());

    @Override
    public Duration getRate() {
        return adConfig.getRate();
    }

    @Override
    public void run() {
        Channel channel = new ChannelImpl(adConfig, mailService);
        if (!channel.getLastBuildDate().isAfter(lastBuildDate)) {
            log.info("{} is already parsed on {}", channel, channel.getLastBuildDate());
            return;
        }
        log.debug("All {} ads in the {}: {}", channel.getAds().size(), channel, channel.getAds());

        List<Ad> ads = filter(channel.getAds());
        log.debug("Matched {} ads in the {}: {}", ads.size(), channel, ads);

        if (!ads.isEmpty()) {
            String text = ads.stream().map(Ad::toHtml).collect(Collectors.joining("<br/>"));
            mailService.queue(getClass().getName(), text, true);
        }

        lastBuildDate = channel.getLastBuildDate();
    }

    private List<Ad> filter(List<Ad> ads) {
        return ads.stream()
                .filter(ad -> ad.getPubDate().isAfter(lastBuildDate))
                .filter(ad -> (boolean) adConfig.getExpression().getValue(ad))
                .collect(Collectors.toList());
    }
}
