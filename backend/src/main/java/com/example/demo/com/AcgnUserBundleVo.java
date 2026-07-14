package com.example.demo.com;

import java.util.List;
import java.util.Map;

public class AcgnUserBundleVo {
    private List<AcgnLibraryItemVo> library;
    private List<AcgnSubscribeItemVo> subscriptions;
    private Map<String, Object> progress;

    public List<AcgnLibraryItemVo> getLibrary() {
        return library;
    }

    public void setLibrary(List<AcgnLibraryItemVo> library) {
        this.library = library;
    }

    public List<AcgnSubscribeItemVo> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<AcgnSubscribeItemVo> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Map<String, Object> getProgress() {
        return progress;
    }

    public void setProgress(Map<String, Object> progress) {
        this.progress = progress;
    }
}
