package dev.hugosmp.auctionhouse.price;

import com.google.gson.annotations.SerializedName;

/** A single observed auction listing parsed from an auction GUI slot. */
public final class Listing {
    @SerializedName("item")
    public final String itemId;
    @SerializedName("component_hash")
    public final int componentHash;
    @SerializedName("price")
    public final long price;
    @SerializedName("count")
    public final int stackCount;
    @SerializedName("seen")
    public final long seenAtMs;

    public Listing(String itemId, int componentHash, long price, int stackCount, long seenAtMs) {
        this.itemId = itemId;
        this.componentHash = componentHash;
        this.price = price;
        this.stackCount = stackCount;
        this.seenAtMs = seenAtMs;
    }
}
