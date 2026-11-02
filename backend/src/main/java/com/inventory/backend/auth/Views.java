package com.inventory.backend.auth;

/**
 * JSON visibility levels. Fields marked @JsonView(Views.Admin.class), such as
 * costs and profit, are left out of every response sent to a seller.
 */
public final class Views {

    private Views() {}

    public interface Seller {}

    public interface Admin extends Seller {}
}
