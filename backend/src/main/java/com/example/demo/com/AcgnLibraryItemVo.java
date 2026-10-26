package com.example.demo.com;

public class AcgnLibraryItemVo {
    private String type;
    private int id;
    private String name;
    private String pic;
    private long at;
    private boolean starred;
    private Long starredAt;

    public AcgnLibraryItemVo() {}

    public AcgnLibraryItemVo(
            String type, int id, String name, String pic, long at, boolean starred, Long starredAt) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.pic = pic;
        this.at = at;
        this.starred = starred;
        this.starredAt = starredAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public long getAt() {
        return at;
    }

    public void setAt(long at) {
        this.at = at;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public Long getStarredAt() {
        return starredAt;
    }

    public void setStarredAt(Long starredAt) {
        this.starredAt = starredAt;
    }
}
