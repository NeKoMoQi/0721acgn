package com.example.demo.com;

public class AcgnSubscribeRow {
    private String clientId;
    private String mediaType;
    private int mediaId;
    private String title;
    private String pic;
    private long subscribedAt;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public int getMediaId() {
        return mediaId;
    }

    public void setMediaId(int mediaId) {
        this.mediaId = mediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public long getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(long subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
}
