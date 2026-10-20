package com.saicharan.expensetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConfig {

    /**
     * Use the driver's java.time codecs so LocalDate is stored as UTC midnight.
     * The default Spring converter uses the JVM time zone, which made dates shift
     * by a day when the API and report-service ran in different zones.
     */
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(config -> config.useNativeDriverJavaTimeCodecs());
    }
}
