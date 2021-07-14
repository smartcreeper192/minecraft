// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib;

import org.apache.logging.log4j.LogManager;
import java.util.Arrays;
import java.util.function.Function;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import java.util.Optional;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

public class EnvironmentParser
{
    @Nullable
    private static String environmentOverride;
    private static final String PROP_PREFIX = "minecraft.api.";
    private static final Logger LOGGER;
    public static final String PROP_ENV = "minecraft.api.env";
    public static final String PROP_AUTH_HOST = "minecraft.api.auth.host";
    public static final String PROP_ACCOUNT_HOST = "minecraft.api.account.host";
    public static final String PROP_SESSION_HOST = "minecraft.api.session.host";
    public static final String PROP_SERVICES_HOST = "minecraft.api.services.host";
    
    public static void setEnvironmentOverride(final String override) {
        EnvironmentParser.environmentOverride = override;
    }
    
    public static Optional<Environment> getEnvironmentFromProperties() {
        final String envName = (EnvironmentParser.environmentOverride != null) ? EnvironmentParser.environmentOverride : System.getProperty("minecraft.api.env");
        final Optional<Environment> env = YggdrasilEnvironment.fromString(envName).map((Function<? super YggdrasilEnvironment, ? extends Environment>)Environment.class::cast);
        return env.isPresent() ? env : fromHostNames();
    }
    
    private static Optional<Environment> fromHostNames() {
        final String auth = System.getProperty("minecraft.api.auth.host");
        final String account = System.getProperty("minecraft.api.account.host");
        final String session = System.getProperty("minecraft.api.session.host");
        final String services = System.getProperty("minecraft.api.services.host");
        if (auth != null && account != null && session != null) {
            return Optional.of(Environment.create(auth, account, session, services, "properties"));
            //return Optional.of(Environment.create(auth, account, session, services, "properties"));
        }
        if (auth != null || account != null || session != null) {
            EnvironmentParser.LOGGER.info("Ignoring hosts properties. All need to be set: " + Arrays.asList("minecraft.api.auth.host", "minecraft.api.account.host", "minecraft.api.session.host"));
        }
        return Optional.empty();
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
