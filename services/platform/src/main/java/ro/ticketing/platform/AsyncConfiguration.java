package ro.ticketing.platform;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Turns on @Async methods (e.g. sending emails without keeping the request waiting). */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfiguration {
}
