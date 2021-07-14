// 
// Decompiled by Procyon v0.6-prerelease
// 

package com.mojang.authlib.yggdrasil.response;

public class PrivilegesResponse extends Response
{
    private Privileges privileges;
    
    public PrivilegesResponse() {
        this.privileges = new Privileges();
    }
    
    public Privileges getPrivileges() {
        return this.privileges;
    }
    
    public class Privileges
    {
        private Privilege onlineChat;
        private Privilege multiplayerServer;
        private Privilege multiplayerRealms;
        
        public Privileges() {
            this.onlineChat = new Privilege();
            this.multiplayerServer = new Privilege();
            this.multiplayerRealms = new Privilege();
        }
        
        public Privilege getOnlineChat() {
            return this.onlineChat;
        }
        
        public Privilege getMultiplayerServer() {
            return this.multiplayerServer;
        }
        
        public Privilege getMultiplayerRealms() {
            return this.multiplayerRealms;
        }
        
        public class Privilege
        {
            private boolean enabled;
            
            public boolean isEnabled() {
                return this.enabled;
            }
        }
    }
}
