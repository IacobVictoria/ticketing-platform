package ro.ticketing.platform;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods (e.g. the nightly refresh token cleanup). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class SchedulingConfiguration {
}
