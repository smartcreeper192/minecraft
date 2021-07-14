// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.Agent;

public class AuthenticationRequest
{
    private Agent agent;
    private String username;
    private String password;
    private String clientToken;
    private boolean requestUser;
    
    public AuthenticationRequest(final Agent agent, final String username, final String password, final String clientToken) {
        this.requestUser = true;
        this.agent = agent;
        this.username = username;
        this.password = password;
        this.clientToken = clientToken;
    }
}
