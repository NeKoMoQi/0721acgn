package com.example.demo.dto;

/** 足迹访问 / 订阅 / 进度等通用的作品定位 */
public class AcgnMediaTouchDto {
    private String mediaType;
    private Integer mediaId;
    private String name;
    private String pic;

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Integer getMediaId() {
        return mediaId;
    }

    public void setMediaId(Integer mediaId) {
        this.mediaId = mediaId;
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
}
