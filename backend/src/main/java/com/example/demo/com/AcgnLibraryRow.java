package com.example.demo.com;

/** MyBatis tb_acgn_library row */
public class AcgnLibraryRow {
    private String clientId;
    private String mediaType;
    private int mediaId;
    private String title;
    private String pic;
    private int starred;
    private Long starredAt;
    private long lastVisitAt;

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

    public int getStarred() {
        return starred;
    }

    public void setStarred(int starred) {
        this.starred = starred;
    }

    public Long getStarredAt() {
        return starredAt;
    }

    public void setStarredAt(Long starredAt) {
        this.starredAt = starredAt;
    }

    public long getLastVisitAt() {
        return lastVisitAt;
    }

    public void setLastVisitAt(long lastVisitAt) {
        this.lastVisitAt = lastVisitAt;
    }
}
