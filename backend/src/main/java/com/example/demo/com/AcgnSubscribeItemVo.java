package com.example.demo.com;

public class AcgnSubscribeItemVo {
    private String type;
    private int id;
    private String name;
    private String pic;
    private long subscribedAt;

    public AcgnSubscribeItemVo() {}

    public AcgnSubscribeItemVo(String type, int id, String name, String pic, long subscribedAt) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.pic = pic;
        this.subscribedAt = subscribedAt;
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

    public long getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(long subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
}
