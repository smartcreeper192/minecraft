// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil;

import java.util.stream.Stream;
import java.util.Optional;
import javax.annotation.Nullable;
import java.util.StringJoiner;
import com.mojang.authlib.Environment;

public enum YggdrasilEnvironment implements Environment
{
    PROD("https://authserver.mojang.com", "https://api.mojang.com", "https://sessionserver.mojang.com", "https://api.minecraftservices.com"), 
    STAGING("https://yggdrasil-auth-staging.mojang.com", "https://api-staging.mojang.com", "https://yggdrasil-auth-session-staging.mojang.zone", "https://api-staging.minecraftservices.com");
    
    private final String authHost;
    private final String accountsHost;
    private final String sessionHost;
    private final String servicesHost;
    
    private YggdrasilEnvironment(final String authHost, final String accountsHost, final String sessionHost, final String servicesHost) {
        this.authHost = authHost;
        this.accountsHost = accountsHost;
        this.sessionHost = sessionHost;
        this.servicesHost = servicesHost;
    }
    
    @Override
    public String getAuthHost() {
        return this.authHost;
    }
    
    @Override
    public String getAccountsHost() {
        return this.accountsHost;
    }
    
    @Override
    public String getSessionHost() {
        return this.sessionHost;
    }
    
    @Override
    public String getServicesHost() {
        return this.servicesHost;
    }
    
    @Override
    public String getName() {
        return this.name();
    }
    
    @Override
    public String asString() {
        return new StringJoiner(", ", "", "").add("authHost='" + this.authHost + "'").add("accountsHost='" + this.accountsHost + "'").add("sessionHost='" + this.sessionHost + "'").add("servicesHost='" + this.servicesHost + "'").add("name='" + this.getName() + "'").toString();
    }
    
    public static Optional<YggdrasilEnvironment> fromString(@Nullable final String value) {
        return Stream.of(values()).filter(env -> value != null && value.equalsIgnoreCase(env.name())).findFirst();
    }
}
